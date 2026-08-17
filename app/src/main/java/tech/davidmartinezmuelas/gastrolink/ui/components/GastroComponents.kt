package tech.davidmartinezmuelas.gastrolink.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCalories
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCarbs
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionFat
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionProtein
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape

// =============================================================================
// KcalChip — Chip ambar con calorias, sobreimpresionable sobre imagenes
// =============================================================================

@Composable
fun KcalChip(
    kcal: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = NutritionCalories,
        shadowElevation = 2.dp
    ) {
        Text(
            text = "$kcal kcal",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// =============================================================================
// MacroPill — Pill individual de un macro (P / C / G)
// =============================================================================

@Composable
fun MacroPill(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = color.copy(alpha = 0.13f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
        }
    }
}

// =============================================================================
// MacroPillsRow — Fila con los 3 macros (proteina, carbos, grasa)
// =============================================================================

@Composable
fun MacroPillsRow(
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MacroPill(value = "${proteinG}g", label = "P", color = NutritionProtein)
        MacroPill(value = "${carbsG}g",  label = "C", color = NutritionCarbs)
        MacroPill(value = "${fatG}g",    label = "G", color = NutritionFat)
    }
}

// =============================================================================
// NutritionStatCard — Card de una metrica nutricional con color semantico
// =============================================================================

@Composable
fun NutritionStatCard(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val progressAnim by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )
    LaunchedEffect(progress) {
        animatedProgress = progress?.coerceIn(0f, 1f) ?: 0f
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(GastroSpacing.md)) {
            // Indicador de color + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(GastroSpacing.sm))
            // Valor + unidad
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            // Barra de progreso opcional (animada)
            if (progress != null) {
                Spacer(Modifier.height(GastroSpacing.sm))
                LinearProgressIndicator(
                    progress = { progressAnim },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(PillShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// =============================================================================
// NutritionStatGrid — Grid 2x2 de los 4 macros (uso directo en Cart, Summary)
// =============================================================================

@Composable
fun NutritionStatGrid(
    kcal: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    kcalProgress: Float? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)) {
            NutritionStatCard(
                label = "Calo",
                value = "$kcal",
                unit = "kcal",
                color = NutritionCalories,
                modifier = Modifier.weight(1f),
                progress = kcalProgress
            )
            NutritionStatCard(
                label = "Chất đạm",
                value = "$proteinG",
                unit = "g",
                color = NutritionProtein,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)) {
            NutritionStatCard(
                label = "Carbs",
                value = "$carbsG",
                unit = "g",
                color = NutritionCarbs,
                modifier = Modifier.weight(1f)
            )
            NutritionStatCard(
                label = "Chất béo",
                value = "$fatG",
                unit = "g",
                color = NutritionFat,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// SectionHeader — Encabezado de seccion con estilo label uppercase primary
// =============================================================================

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GastroSpacing.md, vertical = GastroSpacing.xs)
    )
}

// =============================================================================
// EmptyState — Estado vacio con icono, titulo, subtitulo y accion opcional
// =============================================================================

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(GastroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(GastroSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(GastroSpacing.sm))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(GastroSpacing.lg))
            Button(onClick = onAction, shape = MaterialTheme.shapes.large) {
                Text(actionLabel)
            }
        }
    }
}

// =============================================================================
// LoadingState — Estado de carga con spinner y mensaje
// =============================================================================

@Composable
fun LoadingState(
    message: String = "Đang tải...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(GastroSpacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// QuantityControl — Botones +/- con cantidad para carrito / detalle de plato
// =============================================================================

@Composable
fun QuantityControl(
    qty: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalIconButton(
            onClick = onDecrease,
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "-",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = "$qty",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 28.dp),
            textAlign = TextAlign.Center
        )
        FilledTonalIconButton(
            onClick = onIncrease,
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// =============================================================================
// MacroDistributionBar — Barra de progreso horizontal con label y porcentaje
// =============================================================================

@Composable
fun MacroDistributionBar(
    label: String,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animatedPct by remember { mutableFloatStateOf(0f) }
    val pctAnim by animateFloatAsState(
        targetValue = animatedPct,
        animationSpec = tween(durationMillis = 900),
        label = "macroPct"
    )
    LaunchedEffect(percentage) { animatedPct = percentage.coerceIn(0f, 1f) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { pctAnim },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(PillShape),
            color = color,
            trackColor = color.copy(alpha = 0.13f)
        )
    }
}
