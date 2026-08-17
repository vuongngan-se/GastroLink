package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import coil.compose.AsyncImage
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.davidmartinezmuelas.gastrolink.domain.RecommendationSource
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.Participant
import tech.davidmartinezmuelas.gastrolink.ui.components.KcalChip
import tech.davidmartinezmuelas.gastrolink.ui.components.MacroPillsRow
import tech.davidmartinezmuelas.gastrolink.ui.components.NutritionStatGrid
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SummaryScreen(
    nutritionMode: NutritionMode,
    totals: NutritionTotals,
    recommendations: List<String>,
    recommendationSource: RecommendationSource,
    isRecommendationLoading: Boolean,
    participants: List<Participant>,
    totalsByParticipant: Map<String, NutritionTotals>,
    isSavingOrder: Boolean,
    totalAmount: Int = 0,
    allergyDishes: List<String> = emptyList(),
    onConfirmOrder: (address: String, phoneNumber: String, paymentMethod: String) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val participantNames = participants.associateBy({ it.id }, { it.name })
    var showAllergenDialog by remember { mutableStateOf(false) }

    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPON_ARRIVAL") } // "QR_CODE" or "UPON_ARRIVAL"
    var showErrors by remember { mutableStateOf(false) }

    val isAddressError = showErrors && address.isBlank()
    val isPhoneError = showErrors && phoneNumber.isBlank()

    Scaffold(
        topBar = {
            GastroTopBar(
                title = "Tóm tắt đơn hàng",
                onBack = onBack,
                onSettings = onOpenSettings
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = GastroSpacing.md,
                vertical = GastroSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.lg)
        ) {
            if (allergyDishes.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GastroSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                        ) {
                            Text(
                                text = "🚨",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PHÁT HIỆN CHẤT GÂY DỊ ỨNG NGUY HIỂM!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Đơn hàng chứa các món có nguyên liệu gây dị ứng: ${allergyDishes.joinToString(", ")}. Vui lòng kiểm tra kỹ hoặc hủy đặt để đảm bảo sức khỏe!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            if (totalAmount > 0) {
                item {
                    SectionHeader(title = "Tổng thanh toán")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GastroSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Số tiền cần thanh toán",
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

            item {
                SectionHeader(title = "Tổng giá trị dinh dưỡng")
            }
            item {
                NutritionStatGrid(
                    kcal = totals.kcal,
                    proteinG = totals.proteinG,
                    carbsG = totals.carbsG,
                    fatG = totals.fatG
                )
            }

            if (totalsByParticipant.isNotEmpty()) {
                item {
                    SectionHeader(title = "Theo từng thành viên")
                }

                val orderedEntries = participants
                    .mapNotNull { p -> totalsByParticipant[p.id]?.let { p.id to it } }
                    .ifEmpty { totalsByParticipant.entries.map { it.key to it.value } }

                items(orderedEntries, key = { it.first }) { (id, pTotals) ->
                    val label = participantNames[id] ?: id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GastroSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KcalChip(kcal = pTotals.kcal)
                                MacroPillsRow(
                                    proteinG = pTotals.proteinG,
                                    carbsG = pTotals.carbsG,
                                    fatG = pTotals.fatG
                                )
                            }
                        }
                    }
                }
            }

            if (nutritionMode == NutritionMode.WITH_PROFILE) {
                item {
                    SectionHeader(title = "Gợi ý / Đề xuất")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GastroSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            val sourceLabel = when (recommendationSource) {
                                RecommendationSource.AI -> "AI"
                                RecommendationSource.LOCAL_RULES -> "Quy tắc cục bộ"
                                RecommendationSource.NONE -> null
                            }
                            if (sourceLabel != null) {
                                Surface(
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = sourceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            when {
                                isRecommendationLoading -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Đang tạo gợi ý...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                recommendationSource == RecommendationSource.NONE ||
                                recommendations.isEmpty() -> {
                                    Text(
                                        text = "Không có gợi ý nào cho đơn hàng này",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                else -> {
                                    recommendations.forEach { message ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Thông tin giao hàng")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(GastroSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Địa chỉ nhận hàng") },
                            placeholder = { Text("Nhập địa chỉ đầy đủ của bạn") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            isError = isAddressError,
                            supportingText = if (isAddressError) {
                                { Text("Vui lòng nhập địa chỉ nhận hàng", color = MaterialTheme.colorScheme.error) }
                            } else null
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Số điện thoại liên hệ") },
                            placeholder = { Text("Nhập số điện thoại di động") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            isError = isPhoneError,
                            supportingText = if (isPhoneError) {
                                { Text("Vui lòng nhập số điện thoại liên hệ", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            )
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Phương thức thanh toán")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.md)
                    ) {
                        // Option 1: COD
                        val selectedArrival = paymentMethod == "UPON_ARRIVAL"
                        Card(
                            onClick = { paymentMethod = "UPON_ARRIVAL" },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedArrival) 2.dp else 1.dp,
                                color = if (selectedArrival) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedArrival) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GastroSpacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)
                            ) {
                                Text("💵", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    text = "Khi nhận hàng",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Trả bằng tiền mặt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Option 2: QR
                        val selectedQr = paymentMethod == "QR_CODE"
                        Card(
                            onClick = { paymentMethod = "QR_CODE" },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedQr) 2.dp else 1.dp,
                                color = if (selectedQr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedQr) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GastroSpacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)
                            ) {
                                Text("🔳", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    text = "Mã QR",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Quét để thanh toán",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    if (paymentMethod == "QR_CODE") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GastroSpacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                            ) {
                                Text(
                                    text = "Mã QR Thanh Toán VietQR",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Vietcombank - 1037124080",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(GastroSpacing.xs))
                                AsyncImage(
                                    model = "https://img.vietqr.io/image/vietcombank-1037124080-compact2.png?amount=$totalAmount&addInfo=GastroLink%20Order",
                                    contentDescription = "VietQR Code",
                                    modifier = Modifier
                                        .size(260.dp)
                                        .padding(GastroSpacing.xs)
                                )
                                Spacer(modifier = Modifier.height(GastroSpacing.xs))
                                Text(
                                    text = "Vui lòng quét mã QR trên bằng ứng dụng ngân hàng hoặc ví điện tử để thanh toán số tiền %,d đ".format(totalAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(GastroSpacing.sm))
                Button(
                    onClick = {
                        if (address.isBlank() || phoneNumber.isBlank()) {
                            showErrors = true
                        } else if (allergyDishes.isNotEmpty()) {
                            showAllergenDialog = true
                        } else {
                            onConfirmOrder(address, phoneNumber, paymentMethod)
                        }
                    },
                    enabled = !isSavingOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allergyDishes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isSavingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (allergyDishes.isNotEmpty()) "Vẫn tiếp tục đặt món ⚠️" else "Xác nhận đặt món",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(GastroSpacing.xl))
            }
        }
    }

    if (showAllergenDialog) {
        AlertDialog(
            onDismissRequest = { showAllergenDialog = false },
            title = {
                Text(
                    text = "🚨 CẢNH BÁO DỊ ỨNG NGUY HIỂM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Đơn hàng của bạn chứa các món có nguyên liệu gây dị ứng: ${allergyDishes.joinToString(", ")}.\n\nViệc tiêu thụ các món ăn này có thể gây nguy hiểm nghiêm trọng đến sức khỏe của bạn. Bạn có thực sự chắc chắn muốn tiếp tục đặt món không?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAllergenDialog = false
                        onConfirmOrder(address, phoneNumber, paymentMethod)
                    }
                ) {
                    Text(
                        text = "Vẫn đặt món",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAllergenDialog = false }
                ) {
                    Text(text = "Hủy đặt (Khuyên dùng)")
                }
            }
        )
    }
}

@Composable
fun MockQrCode(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val size = this.size.width
        val squareSize = size / 10f
        // Draw background
        drawRect(color = androidx.compose.ui.graphics.Color.White)
        
        // Draw the 3 finder patterns (top-left, top-right, bottom-left)
        fun drawFinderPattern(x: Float, y: Float) {
            // Outer 7x7 square
            drawRect(
                color = androidx.compose.ui.graphics.Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(squareSize * 3, squareSize * 3)
            )
            drawRect(
                color = androidx.compose.ui.graphics.Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(x + squareSize * 0.5f, y + squareSize * 0.5f),
                size = androidx.compose.ui.geometry.Size(squareSize * 2, squareSize * 2)
            )
            drawRect(
                color = androidx.compose.ui.graphics.Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(x + squareSize * 1f, y + squareSize * 1f),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
            )
        }
        
        drawFinderPattern(0f, 0f)
        drawFinderPattern(size - squareSize * 3, 0f)
        drawFinderPattern(0f, size - squareSize * 3)
        
        // Draw some random QR-like noise squares
        val random = java.util.Random(42) // Fixed seed for stable look
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                // Skip finder pattern zones
                if ((i < 3 && j < 3) || (i >= 7 && j < 3) || (i < 3 && j >= 7)) continue
                if (random.nextBoolean()) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(i * squareSize, j * squareSize),
                        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                    )
                }
            }
        }
    }
}
