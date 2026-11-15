package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FoodBridgeLightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = SaffronSecondary,
    onSecondary = Color.White,
    secondaryContainer = MarigoldContainer,
    onSecondaryContainer = OnMarigoldContainer,
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = IvoryBackground,
    onBackground = DeepSlateText,
    surface = PureSurface,
    onSurface = DeepSlateText,
    surfaceVariant = SoftSageSurfaceVariant,
    onSurfaceVariant = MutedSlateText,
    outline = SlateOutline,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorCrimson
)

private val FoodBridgeDarkScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = OnMintContainer,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = MintContainer,
    secondary = DarkSaffronSecondary,
    onSecondary = OnMarigoldContainer,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = MarigoldContainer,
    tertiary = TealContainer,
    onTertiary = OnTealContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF2D4A36),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

val FoodBridgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FoodBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FoodBridgeDarkScheme
        else -> FoodBridgeLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FoodBridgeTypography,
        shapes = FoodBridgeShapes,
        content = content
    )
}
