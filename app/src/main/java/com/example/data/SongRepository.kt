package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import kotlin.math.roundToInt

@Keep
@IgnoreExtraProperties
data class SongDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val fileName: String = "",
    val lyrics: String = "",
    val lyricsSummary: String = "",
    val durationSec: Double = 0.0,
    val sampleRate: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class SongRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user Firestore operation." }
        return uid
    }

    fun observeSongs(userId: String): Flow<List<SongDocument>> = flow {
        val uid = requireSignedInUser(userId)
        val path = "users/$uid/songs"
        emitAll(
            db.collection("users").document(uid).collection("songs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SongDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getSong(userId: String, songId: String): Result<SongDocument?> {
        val path = "users/$userId/songs/$songId"
        return try {
            val uid = requireSignedInUser(userId)
            require(VALID_ID_REGEX.matches(songId)) { "Invalid song document ID." }
            val snap = db.collection("users").document(uid)
                .collection("songs").document(songId)
                .get()
                .await()
            val song = snap.toObject(
                SongDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(song)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Saves modified lyrics (along with current song metadata) to the current song document
     * under /users/{userId}/songs/{songId}. Creates the document if absent, or performs a
     * partial update preserving immutable userId and createdAt fields if it already exists.
     */
    suspend fun saveModifiedLyricsToCurrentSong(
        userId: String,
        songId: String,
        title: String,
        fileName: String,
        modifiedLyrics: String,
        lyricsSummary: String,
        durationSec: Double,
        sampleRate: Long
    ): Result<String> {
        val docPath = "users/$userId/songs/$songId"
        var opType = OperationType.WRITE
        return try {
            val uid = requireSignedInUser(userId)
            require(VALID_ID_REGEX.matches(songId)) {
                "Song ID must be 1..128 alphanumeric, underscore, or hyphen characters."
            }

            val cleanTitle = title.trim().ifEmpty { "Untitled Song" }.take(150)
            val cleanFileName = fileName.trim().take(200)
            val cleanLyrics = modifiedLyrics.take(20000)
            val cleanSummary = lyricsSummary.take(2000)
            val safeDuration = durationSec.coerceIn(0.0, 86400.0)
            val safeSampleRate = sampleRate.coerceIn(0L, 384000L)

            val docRef = db.collection("users").document(uid)
                .collection("songs").document(songId)

            val existingSnapshot = docRef.get().await()
            if (existingSnapshot.exists()) {
                opType = OperationType.UPDATE
                val updatePayload = mapOf(
                    "title" to cleanTitle,
                    "fileName" to cleanFileName,
                    "lyrics" to cleanLyrics,
                    "lyricsSummary" to cleanSummary,
                    "durationSec" to safeDuration,
                    "sampleRate" to safeSampleRate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                opType = OperationType.CREATE
                val createPayload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "fileName" to cleanFileName,
                    "lyrics" to cleanLyrics,
                    "lyricsSummary" to cleanSummary,
                    "durationSec" to safeDuration,
                    "sampleRate" to safeSampleRate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }

            Result.success(songId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, docPath)
            Result.failure(e)
        }
    }

    companion object {
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

        fun buildSongDocumentId(fileName: String, durationSec: Float, sampleRate: Int): String {
            val base = fileName
                .substringBeforeLast('.')
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .trim('_')
                .ifEmpty { "song" }
                .take(40)
            val key = "$fileName|${(durationSec * 10f).roundToInt()}|$sampleRate"
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(key.toByteArray(Charsets.UTF_8))
                .take(6)
                .joinToString("") { "%02x".format(it) }
            return "${base}_$digest"
        }
    }
}
