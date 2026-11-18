package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CafeLightColorScheme = lightColorScheme(
    primary = EspressoBrown,
    onPrimary = WarmWhite,
    primaryContainer = SoftBeige,
    onPrimaryContainer = EspressoBrown,
    secondary = RoastedCaramel,
    onSecondary = WarmWhite,
    secondaryContainer = OatBeige,
    onSecondaryContainer = DeepRoast,
    tertiary = WarmGold,
    onTertiary = DeepRoast,
    tertiaryContainer = SoftAmberBg,
    onTertiaryContainer = DarkMocha,
    background = WarmCream,
    onBackground = DeepRoast,
    surface = WarmWhite,
    onSurface = DeepRoast,
    surfaceVariant = SoftBeige,
    onSurfaceVariant = MutedMocha,
    outline = OatBeige,
    outlineVariant = SoftBeige,
    error = RoastRed,
    onError = WarmWhite,
    errorContainer = SoftRedBg,
    onErrorContainer = RoastRed
)

private val CafeDarkColorScheme = darkColorScheme(
    primary = WarmGold,
    onPrimary = DeepRoast,
    primaryContainer = DarkMocha,
    onPrimaryContainer = LightCreamText,
    secondary = RoastedCaramel,
    onSecondary = WarmWhite,
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = LightCreamText,
    tertiary = WarmGold,
    onTertiary = DeepRoast,
    background = DarkEspressoBg,
    onBackground = LightCreamText,
    surface = DarkMochaSurface,
    onSurface = LightCreamText,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = MutedBeigeText,
    outline = DarkMocha,
    outlineVariant = DarkCardSurface,
    error = RoastRed,
    onError = WarmWhite
)

val CafeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CafeAntonioLunaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CafeDarkColorScheme else CafeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CafeTypography,
        shapes = CafeShapes,
        content = content
    )
}
