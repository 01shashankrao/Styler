package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StylerColorScheme = lightColorScheme(
    primary = StylerDeepGreen,
    onPrimary = StylerPureWhite,
    primaryContainer = StylerDeepGreenContainer,
    onPrimaryContainer = StylerDeepGreen,
    secondary = StylerSageGreen,
    onSecondary = StylerPureWhite,
    secondaryContainer = StylerSageContainer,
    onSecondaryContainer = StylerDeepGreen,
    tertiary = StylerAccentOrange,
    onTertiary = StylerPureWhite,
    tertiaryContainer = StylerOrangeContainer,
    onTertiaryContainer = StylerDarkCharcoal,
    background = StylerLightSurface,
    onBackground = StylerDarkCharcoal,
    surface = StylerCardSurface,
    onSurface = StylerDarkCharcoal,
    surfaceVariant = StylerSageContainer,
    onSurfaceVariant = StylerDarkCharcoal,
    outline = StylerSubtleBorder,
    error = StylerErrorRed,
    onError = StylerPureWhite,
    errorContainer = StylerErrorContainer,
    onErrorContainer = StylerErrorRed
)

val StylerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StylerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StylerColorScheme,
        typography = Typography,
        shapes = StylerShapes,
        content = content
    )
}
