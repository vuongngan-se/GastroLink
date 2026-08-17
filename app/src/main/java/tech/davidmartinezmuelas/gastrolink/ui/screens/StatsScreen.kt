package tech.davidmartinezmuelas.gastrolink.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.davidmartinezmuelas.gastrolink.ui.StatsUi
import tech.davidmartinezmuelas.gastrolink.ui.components.EmptyState
import tech.davidmartinezmuelas.gastrolink.ui.components.MacroDistributionBar
import tech.davidmartinezmuelas.gastrolink.ui.components.NutritionStatGrid
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCalories
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCarbs
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionFat
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionProtein

@Composable
fun StatsScreen(
    stats: StatsUi,
    onBack: () -> Unit
) {
    val hasData = stats.averageCaloriesPerOrder > 0.0

    Scaffold(
        topBar = {
            GastroTopBar(
                title = "Thống kê",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        if (!hasData) {
            EmptyState(
                icon     = Icons.Filled.Star,
                title    = "Chưa có dữ liệu",
                subtitle = "Hãy đặt món để xem thống kê dinh dưỡng của bạn tại đây",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }


        val protCal  = stats.averageProtein * 4.0
        val carbsCal = stats.averageCarbs   * 4.0
        val fatCal   = stats.averageFat     * 9.0
        val totalMacroCal = protCal + carbsCal + fatCal

        val protPct  = if (totalMacroCal > 0) (protCal  / totalMacroCal).toFloat() else 0f
        val carbsPct = if (totalMacroCal > 0) (carbsCal / totalMacroCal).toFloat() else 0f
        val fatPct   = if (totalMacroCal > 0) (fatCal   / totalMacroCal).toFloat() else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GastroSpacing.md, vertical = GastroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.lg)
        ) {


            SectionHeader(title = "Trung bình mỗi đơn hàng")

            NutritionStatGrid(
                kcal     = stats.averageCaloriesPerOrder.toInt(),
                proteinG = stats.averageProtein.toInt(),
                carbsG   = stats.averageCarbs.toInt(),
                fatG     = stats.averageFat.toInt()
            )

            RecentCaloriesChart(recentCalories = stats.recentCalories)


            SectionHeader(title = "Phân bổ chất dinh dưỡng")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.large,
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                ) {
                    Text(
                        text  = "Tỷ lệ calo từ mỗi loại chất dinh dưỡng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MacroDistributionBar(
                        label      = "Chất đạm (Protein)",
                        percentage = protPct,
                        color      = NutritionProtein
                    )
                    MacroDistributionBar(
                        label      = "Carbohydrate (Carbs)",
                        percentage = carbsPct,
                        color      = NutritionCarbs
                    )
                    MacroDistributionBar(
                        label      = "Chất béo (Fat)",
                        percentage = fatPct,
                        color      = NutritionFat
                    )
                }
            }


            if (stats.mostOrderedDishName.isNotBlank() &&
                stats.mostOrderedDishName != "No disponible" &&
                stats.mostOrderedDishName != "Không có dữ liệu"
            ) {
                SectionHeader(title = "Món ăn được đặt nhiều nhất")

                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = MaterialTheme.shapes.large,
                    colors    = CardDefaults.cardColors(
                        containerColor = NutritionCalories.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GastroSpacing.md),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                    ) {
                        // Trofeo
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(NutritionCalories.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint     = NutritionCalories,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text  = "Món ăn yêu thích của bạn",
                                style = MaterialTheme.typography.labelSmall,
                                color = NutritionCalories,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text  = stats.mostOrderedDishName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(GastroSpacing.xl))
        }
    }
}

@Composable
fun RecentCaloriesChart(
    recentCalories: List<Int>,
    modifier: Modifier = Modifier
) {
    if (recentCalories.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GastroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            Text(
                text = "Lịch sử Calo tiêu thụ gần đây 📈",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxCal = (recentCalories.maxOrNull() ?: 1000).coerceAtLeast(1)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(vertical = GastroSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md),
                verticalAlignment = Alignment.Bottom
            ) {
                recentCalories.forEachIndexed { index, cal ->
                    val ratio = cal.toFloat() / maxCal
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${cal}kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(ratio.coerceAtLeast(0.01f))
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Text(
                            text = "Đơn ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
