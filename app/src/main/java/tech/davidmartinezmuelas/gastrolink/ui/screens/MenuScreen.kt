package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import tech.davidmartinezmuelas.gastrolink.model.CartItem
import tech.davidmartinezmuelas.gastrolink.model.Dish
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.model.Participant
import tech.davidmartinezmuelas.gastrolink.ui.components.EmptyState
import tech.davidmartinezmuelas.gastrolink.ui.components.KcalChip
import tech.davidmartinezmuelas.gastrolink.ui.components.MacroPillsRow
import tech.davidmartinezmuelas.gastrolink.ui.components.NutritionStatGrid
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape

// =============================================================================
// DishCard — Tarjeta editorial de un plato
// =============================================================================

@Composable
private fun DishCard(
    dish: Dish,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onViewDetail: () -> Unit
) {
    // Color animado del borde cuando hay items en el carrito
    val borderColor by animateColorAsState(
        targetValue = if (qtyInCart > 0) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(300),
        label = "borderColor"
    )

    Card(
        onClick = onViewDetail,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (qtyInCart > 0) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        // Imagen + overlays
        Box {
            if (!dish.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = dish.imageUrl,
                    contentDescription = dish.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder si no hay imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dish.name.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // KcalChip sobre imagen (esquina inferior izquierda)
            KcalChip(
                kcal = dish.kcal,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )

            // Badge de cantidad (esquina superior derecha)
            if (qtyInCart > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = PillShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "\u00d7$qtyInCart",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Contenido de la card
        Column(
            modifier = Modifier.padding(horizontal = GastroSpacing.md, vertical = GastroSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (dish.price > 0) {
                    Text(
                        text = "%,d đ".format(dish.price),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = GastroSpacing.sm)
                    )
                }
            }

            MacroPillsRow(
                proteinG = dish.proteinG,
                carbsG = dish.carbsG,
                fatG = dish.fatG
            )

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (qtyInCart == 0) "Thêm vào giỏ hàng" else "Thêm phần nữa",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// =============================================================================
// DishDetailSheet — Bottom sheet de detalle completo de un plato
// =============================================================================

@Composable
private fun DishDetailSheet(
    dish: Dish,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Imagen con KcalChip overlay
            Box {
                if (!dish.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = dish.imageUrl,
                        contentDescription = dish.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                KcalChip(
                    kcal = dish.kcal,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = GastroSpacing.lg, vertical = GastroSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
            ) {
                // Nombre del plato
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (dish.price > 0) {
                        Text(
                            text = "%,d đ".format(dish.price),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = GastroSpacing.sm)
                        )
                    }
                }

                // Grid de macros con colores sem\u00e1nticos
                SectionHeader(title = "Thành phần dinh dưỡng")
                NutritionStatGrid(
                    kcal = dish.kcal,
                    proteinG = dish.proteinG,
                    carbsG = dish.carbsG,
                    fatG = dish.fatG
                )

                // Ingredientes
                val ingredientList = dish.ingredients.orEmpty()
                if (ingredientList.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionHeader(title = "Thành phần nguyên liệu")
                    Column(verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)) {
                        ingredientList.forEach { ingredient ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            PillShape
                                        )
                                )
                                Text(
                                    text = ingredient,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Bot\u00f3n a\u00f1adir al carrito
                Spacer(Modifier.height(GastroSpacing.sm))
                Button(
                    onClick = { onAdd(); onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = if (qtyInCart == 0) "Thêm vào giỏ hàng" else "Thêm phần nữa (đã có \u00d7$qtyInCart)",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(GastroSpacing.lg))
            }
        }
    }
}

// =============================================================================
// MenuScreen
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuScreen(
    orderMode: OrderMode?,
    selectedBranchName: String?,
    dishes: List<Dish>,
    cartItems: List<CartItem>,
    participants: List<Participant>,
    selectedParticipantId: String?,
    onSelectParticipant: (String) -> Unit,
    onAddDish: (Dish, String?) -> Unit,
    onGoToCart: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val totalInCart = cartItems.sumOf { it.qty }
    var detailDish by remember { mutableStateOf<Dish?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtrado local por b\u00fasqueda
    val filteredDishes = remember(dishes, searchQuery) {
        if (searchQuery.isBlank()) dishes
        else dishes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            GastroTopBar(
                title = selectedBranchName ?: "Thực đơn",
                subtitle = if (totalInCart > 0) "$totalInCart món trong giỏ hàng" else null,
                onBack = onBack,
                onSettings = onOpenSettings,
                actions = {
                    IconButton(onClick = onGoToCart) {
                        BadgedBox(
                            badge = {
                                if (totalInCart > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = totalInCart.toString(),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Xem giỏ hàng",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (dishes.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = "Không có món ăn khả dụng",
                subtitle = "Vui lòng kiểm tra kết nối mạng hoặc chọn chi nhánh khác",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = GastroSpacing.md,
                    end = GastroSpacing.md,
                    top = GastroSpacing.sm,
                    bottom = GastroSpacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
            ) {

                item(key = "search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Tìm món ăn...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = PillShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }


                if (orderMode == OrderMode.GROUP && participants.isNotEmpty()) {
                    item(key = "participants") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(GastroSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                            ) {
                                Text(
                                    text = "CHỌN MÓN CHO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)) {
                                    participants.forEach { participant ->
                                        FilterChip(
                                            selected = participant.id == selectedParticipantId,
                                            onClick = { onSelectParticipant(participant.id) },
                                            label = { Text(participant.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty state cuando no hay resultados de b\u00fasqueda
                if (filteredDishes.isEmpty()) {
                    item(key = "no_results") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = GastroSpacing.xxl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                            ) {
                                Text(
                                    text = "Không tìm thấy kết quả cho \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Vui lòng thử tìm tên khác",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Lista de platos
                    items(filteredDishes, key = { it.id }) { dish ->
                        val qtyInCart = if (orderMode == OrderMode.GROUP) {
                            cartItems
                                .filter { it.dish.id == dish.id && it.participantId == selectedParticipantId }
                                .sumOf { it.qty }
                        } else {
                            cartItems.filter { it.dish.id == dish.id }.sumOf { it.qty }
                        }
                        DishCard(
                            dish = dish,
                            qtyInCart = qtyInCart,
                            onAdd = { onAddDish(dish, selectedParticipantId) },
                            onViewDetail = { detailDish = dish }
                        )
                    }
                }
            }
        }
    }


    detailDish?.let { dish ->
        val qtyInCart = if (orderMode == OrderMode.GROUP) {
            cartItems
                .filter { it.dish.id == dish.id && it.participantId == selectedParticipantId }
                .sumOf { it.qty }
        } else {
            cartItems.filter { it.dish.id == dish.id }.sumOf { it.qty }
        }
        DishDetailSheet(
            dish = dish,
            qtyInCart = qtyInCart,
            onAdd = { onAddDish(dish, selectedParticipantId) },
            onDismiss = { detailDish = null }
        )
    }
}
