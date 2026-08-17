package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCalories
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape

@Composable
fun PlansScreen(
    isPremiumEnabled: Boolean,
    isDebugBuild: Boolean,
    onActivatePremiumDemo: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            GastroTopBar(
                title = "Các gói dịch vụ",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GastroSpacing.md, vertical = GastroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            Text(
                text = "Chọn gói dịch vụ của bạn",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "So sánh các tính năng có sẵn của từng gói dịch vụ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Plan Free ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (!isPremiumEnabled) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (!isPremiumEnabled) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                elevation = CardDefaults.cardElevation(defaultElevation = if (!isPremiumEnabled) 0.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Free",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!isPremiumEnabled) {
                            Surface(shape = PillShape, color = MaterialTheme.colorScheme.secondary) {
                                Text(
                                    text = "Gói hiện tại",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = GastroSpacing.sm, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PlanFeature(included = true,  text = "Thông tin dinh dưỡng của từng món ăn")
                    PlanFeature(included = true,  text = "Tổng dinh dưỡng trong giỏ hàng (kcal, đạm, carbs, chất béo)")
                    PlanFeature(included = true,  text = "Lịch sử đặt món")
                    PlanFeature(included = false, text = "Hồ sơ dinh dưỡng cá nhân hóa")
                    PlanFeature(included = false, text = "Gợi ý món ăn dựa trên hồ sơ")
                    PlanFeature(included = false, text = "Gợi ý bằng trí tuệ nhân tạo AI (beta)")
                    PlanFeature(included = false, text = "Thống kê nâng cao")
                }
            }

            // ── Plan Premium ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumEnabled) {
                        NutritionCalories.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (isPremiumEnabled) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, NutritionCalories)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                elevation = CardDefaults.cardElevation(defaultElevation = if (isPremiumEnabled) 0.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = NutritionCalories,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Premium",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPremiumEnabled) NutritionCalories
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isPremiumEnabled) {
                            Surface(shape = PillShape, color = NutritionCalories) {
                                Text(
                                    text = "Gói hiện tại",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(horizontal = GastroSpacing.sm, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PlanFeature(included = true, text = "Bao gồm tất cả tính năng của gói Free")
                    PlanFeature(included = true, text = "Hồ sơ dinh dưỡng cá nhân hóa")
                    PlanFeature(included = true, text = "Gợi ý món ăn dựa trên hồ sơ và mục tiêu")
                    PlanFeature(included = true, text = "Gợi ý bằng trí tuệ nhân tạo AI (beta)")
                    PlanFeature(included = true, text = "Thống kê lịch sử nâng cao")
                }
            }

            // ── CTA ─────────────────────────────────────────────────────
            Button(
                onClick  = onActivatePremiumDemo,
                enabled  = !isPremiumEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = NutritionCalories,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (isPremiumEnabled) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = GastroSpacing.sm)
                            .size(18.dp)
                    )
                }
                Text(
                    text       = if (isPremiumEnabled) "Đã kích hoạt Premium Demo" else "Kích hoạt Premium Demo (miễn phí)",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isDebugBuild) {
                    "Chế độ chạy thử: kích hoạt chỉ là demo và không yêu cầu thanh toán thực tế."
                } else {
                    "Bản demo giới thiệu: không có giao dịch thực tế trong giai đoạn này."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(GastroSpacing.xl))
        }
    }
}

@Composable
private fun PlanFeature(included: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (included) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (included) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
