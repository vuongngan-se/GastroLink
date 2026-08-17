package tech.davidmartinezmuelas.gastrolink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ── Spacing system (8dp grid) ─────────────────────────────────────────────────
object GastroSpacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

// ── Shapes ────────────────────────────────────────────────────────────────────
// PillShape: para chips, badges y barras de progreso
val PillShape = RoundedCornerShape(50.dp)

private val GastroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColorScheme = lightColorScheme(
    primary              = MgPrimary,
    onPrimary            = MgOnPrimary,
    primaryContainer     = MgPrimaryContainer,
    onPrimaryContainer   = MgOnPrimaryContainer,
    secondary            = MgSecondary,
    onSecondary          = MgOnSecondary,
    secondaryContainer   = MgSecondaryContainer,
    onSecondaryContainer = MgOnSecondaryContainer,
    tertiary             = MgTertiary,
    onTertiary           = MgOnTertiary,
    tertiaryContainer    = MgTertiaryContainer,
    onTertiaryContainer  = MgOnTertiaryContainer,
    background           = MgBackground,
    onBackground         = MgOnBackground,
    surface              = MgSurface,
    onSurface            = MgOnSurface,
    surfaceVariant       = MgSurfaceVariant,
    onSurfaceVariant     = MgOnSurfaceVariant,
    outline              = MgOutline,
    outlineVariant       = MgOutlineVariant,
    error                = MgError,
    onError              = MgOnError,
    errorContainer       = MgErrorContainer,
    onErrorContainer     = MgOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary              = MgPrimaryDark,
    onPrimary            = MgOnPrimaryDark,
    primaryContainer     = MgPrimaryContainerDark,
    onPrimaryContainer   = MgOnPrimaryContainerDark,
    secondary            = MgSecondaryDark,
    onSecondary          = MgOnSecondaryDark,
    secondaryContainer   = MgSecondaryContainerDark,
    onSecondaryContainer = MgOnSecondaryContainerDark,
    tertiary             = MgTertiaryDark,
    onTertiary           = MgOnTertiaryDark,
    tertiaryContainer    = MgTertiaryContainerDark,
    onTertiaryContainer  = MgOnTertiaryContainerDark,
    background           = MgBackgroundDark,
    onBackground         = MgOnBackgroundDark,
    surface              = MgSurfaceDark,
    onSurface            = MgOnSurfaceDark,
    surfaceVariant       = MgSurfaceVariantDark,
    onSurfaceVariant     = MgOnSurfaceVariantDark,
    outline              = MgOutlineDark,
    outlineVariant       = MgOutlineVariantDark,
    error                = MgErrorDark,
    onError              = MgOnErrorDark,
    errorContainer       = MgErrorContainerDark,
    onErrorContainer     = MgOnErrorContainerDark
)

@Composable
fun GastroLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = GastroShapes,
        content     = content
    )
}
