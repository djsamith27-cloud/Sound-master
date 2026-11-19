package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val SwaraShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun SwaraStudioTheme(
    isSinhala: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SwaraDarkColorScheme,
        typography = swaraTypography(isSinhala),
        shapes = SwaraShapes,
        content = content
    )
}
