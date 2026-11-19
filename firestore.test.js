const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read, list, create, or update song documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("songs").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("song_1")
      .set({
        userId: ALICE_UID,
        title: "Unauthorized Song",
        fileName: "song.mp3",
        lyrics: "Test lyrics",
        lyricsSummary: "",
        durationSec: 120.0,
        sampleRate: 44100,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Authenticated owner: valid Kotlin song create, partial lyrics update, and list query succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror SongRepository create payload (NO @DocumentId `id` field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("song_1")
      .set({
        userId: ALICE_UID,
        title: "Hiru Sandu",
        fileName: "hiru_sandu.mp3",
        lyrics: "හිරු සඳු පායන අහසේ",
        lyricsSummary: "",
        durationSec: 215.5,
        sampleRate: 44100,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Mirror SongRepository saveModifiedLyricsToCurrentSong partial update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("song_1")
      .update({
        title: "Hiru Sandu",
        fileName: "hiru_sandu.mp3",
        lyrics: "හිරු සඳු පායන අහසේ\nනිවැරදි කළ පද",
        lyricsSummary: "අක්ෂර වින්‍යාස වැරදි නිවැරදි කරන ලදී.",
        durationSec: 215.5,
        sampleRate: 44100,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Mirror SongRepository observeSongs list query
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("songs").get()
  );
});

test("Security bounds: reject cross-user access, ghost fields, immutable createdAt mutation, and invalid IDs", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("songs")
      .doc("bob_song")
      .set({
        userId: BOB_UID,
        title: "Bob Song",
        fileName: "bob.wav",
        lyrics: "Bob lyrics",
        lyricsSummary: "",
        durationSec: 180.0,
        sampleRate: 48000,
        createdAt: created,
        updatedAt: created,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cross-user read/write/list must fail
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("songs").doc("bob_song").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("songs").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("songs")
      .doc("bob_song")
      .update({ lyrics: "Hijacked", updatedAt: new Date(Date.now() - 1000) })
  );

  // Create Alice's own song then test immutable createdAt & ghost field & future timestamp
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("alice_song")
      .set({
        userId: ALICE_UID,
        title: "Alice Song",
        fileName: "alice.wav",
        lyrics: "Draft",
        lyricsSummary: "",
        durationSec: 90.0,
        sampleRate: 44100,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject mutating createdAt
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("alice_song")
      .update({
        lyrics: "Edited",
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject ghost field on update
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("alice_song")
      .update({
        lyrics: "Edited",
        isVerified: true,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject @DocumentId 'id' field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("song_with_id")
      .set({
        id: "song_with_id",
        userId: ALICE_UID,
        title: "Bad ID",
        fileName: "",
        lyrics: "",
        lyricsSummary: "",
        durationSec: 0,
        sampleRate: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject invalid document ID with spaces/dots
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("bad song.id")
      .set({
        userId: ALICE_UID,
        title: "Invalid Doc ID",
        fileName: "",
        lyrics: "",
        lyricsSummary: "",
        durationSec: 0,
        sampleRate: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("songs")
      .doc("future_song")
      .set({
        userId: ALICE_UID,
        title: "Future",
        fileName: "",
        lyrics: "",
        lyricsSummary: "",
        durationSec: 0,
        sampleRate: 0,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});
