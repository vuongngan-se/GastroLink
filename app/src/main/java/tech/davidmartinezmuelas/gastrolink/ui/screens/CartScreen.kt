package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import tech.davidmartinezmuelas.gastrolink.model.CartItem
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.model.Participant
import tech.davidmartinezmuelas.gastrolink.ui.components.EmptyState
import tech.davidmartinezmuelas.gastrolink.ui.components.KcalChip
import tech.davidmartinezmuelas.gastrolink.ui.components.MacroPillsRow
import tech.davidmartinezmuelas.gastrolink.ui.components.NutritionStatGrid
import tech.davidmartinezmuelas.gastrolink.ui.components.QuantityControl
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing

// =============================================================================
// CartItemCard — Tarjeta premium con miniatura 72dp, macros y cantidad
// =============================================================================

@Composable
private fun CartItemCard(
    item: CartItem,
    onIncrease: (String, String?) -> Unit,
    onDecrease: (String, String?) -> Unit
) {
    val totalKcal   = item.dish.kcal      * item.qty
    val totalProt   = item.dish.proteinG  * item.qty
    val totalCarbs  = item.dish.carbsG    * item.qty
    val totalFat    = item.dish.fatG      * item.qty
    val totalPrice  = item.dish.price     * item.qty

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GastroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            // Miniatura 72dp
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!item.dish.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.dish.imageUrl,
                        contentDescription = item.dish.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = item.dish.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Contenido
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)
            ) {
                Text(
                    text = item.dish.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.dish.price > 0) {
                    Text(
                        text = "%,d đ".format(totalPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                KcalChip(kcal = totalKcal)
                MacroPillsRow(
                    proteinG = totalProt,
                    carbsG   = totalCarbs,
                    fatG     = totalFat
                )
            }

            // Control de cantidad
            QuantityControl(
                qty       = item.qty,
                onIncrease = { onIncrease(item.dish.id, item.participantId) },
                onDecrease = { onDecrease(item.dish.id, item.participantId) }
            )
        }
    }
}

// =============================================================================
// CartScreen
// =============================================================================

@Composable
fun CartScreen(
    orderMode: OrderMode?,
    items: List<CartItem>,
    participants: List<Participant>,
    onIncrease: (String, String?) -> Unit,
    onDecrease: (String, String?) -> Unit,
    onGoToSummary: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val totalKcal    = items.sumOf { it.dish.kcal     * it.qty }
    val totalProtein = items.sumOf { it.dish.proteinG * it.qty }
    val totalCarbs   = items.sumOf { it.dish.carbsG   * it.qty }
    val totalFat     = items.sumOf { it.dish.fatG     * it.qty }
    val totalDishes  = items.sumOf { it.qty }
    val totalAmount  = items.sumOf { it.dish.price   * it.qty }

    Scaffold(
        topBar = {
            GastroTopBar(
                title    = "Giỏ hàng",
                subtitle = if (items.isNotEmpty()) "$totalDishes món ăn" else null,
                onBack   = onBack,
                onSettings = onOpenSettings
            )
        }
    ) { innerPadding ->

        if (items.isEmpty()) {
            EmptyState(
                icon        = Icons.Filled.ShoppingCart,
                title       = "Giỏ hàng trống",
                subtitle    = "Vui lòng thêm món ăn từ thực đơn để tiếp tục",
                actionLabel = "Xem thực đơn",
                onAction    = onBack,
                modifier    = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding  = PaddingValues(
                    start   = GastroSpacing.md,
                    end     = GastroSpacing.md,
                    top     = GastroSpacing.md,
                    bottom  = GastroSpacing.xl
                ),
                verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
            ) {


                if (orderMode == OrderMode.GROUP) {
                    participants.forEach { participant ->
                        val pItems = items.filter { it.participantId == participant.id }
                        if (pItems.isNotEmpty()) {
                            val pKcal  = pItems.sumOf { it.dish.kcal     * it.qty }
                            val pProt  = pItems.sumOf { it.dish.proteinG * it.qty }
                            val pCarbs = pItems.sumOf { it.dish.carbsG   * it.qty }
                            val pFat   = pItems.sumOf { it.dish.fatG     * it.qty }

                            item(key = "header_${participant.id}") {
                                SectionHeader(title = participant.name)
                            }
                            items(pItems, key = { "${it.dish.id}_${it.participantId}" }) { cartItem ->
                                CartItemCard(
                                    item       = cartItem,
                                    onIncrease = onIncrease,
                                    onDecrease = onDecrease
                                )
                            }
                            item(key = "pnuts_${participant.id}") {
                                NutritionStatGrid(
                                    kcal     = pKcal,
                                    proteinG = pProt,
                                    carbsG   = pCarbs,
                                    fatG     = pFat,
                                    modifier = Modifier.padding(vertical = GastroSpacing.sm)
                                )
                            }
                            item(key = "div_${participant.id}") {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(vertical = GastroSpacing.sm),
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    item(key = "global_header") {
                        SectionHeader(title = "Tổng cộng")
                    }
                } else {

                    items(items, key = { "${it.dish.id}_${it.participantId}" }) { cartItem ->
                        CartItemCard(
                            item       = cartItem,
                            onIncrease = onIncrease,
                            onDecrease = onDecrease
                        )
                    }
                }

                // ── Tổng tiền ──────────────────────────────────────────────
                if (totalAmount > 0) {
                    item(key = "total_amount") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GastroSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tổng thanh toán",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "%,d đ".format(totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── Tổng giá trị dinh dưỡng ──────────────────────────────
                item(key = "nutrition_summary") {
                    Spacer(Modifier.height(GastroSpacing.sm))
                    NutritionStatGrid(
                        kcal     = totalKcal,
                        proteinG = totalProtein,
                        carbsG   = totalCarbs,
                        fatG     = totalFat
                    )
                }

                // ── CTA:   ──────────────────────────────────
                item(key = "cta_button") {
                    Spacer(Modifier.height(GastroSpacing.sm))
                    Button(
                        onClick  = onGoToSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = MaterialTheme.shapes.large,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text       = "Xác nhận đặt món",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
