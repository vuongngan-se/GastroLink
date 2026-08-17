package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import tech.davidmartinezmuelas.gastrolink.ui.OrderDetailUi
import tech.davidmartinezmuelas.gastrolink.ui.components.NutritionStatGrid
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape

@Composable
fun OrderDetailScreen(
    detail: OrderDetailUi?,
    onDeleteOrder: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            GastroTopBar(
                title = "Chi tiết đơn hàng",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(GastroSpacing.md),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Không có thông tin chi tiết",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = GastroSpacing.md,
                vertical = GastroSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            // Section: Información del pedido
            item {
                SectionHeader(title = "Thông tin đơn hàng")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(GastroSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chi nhánh",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = detail.branchName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chế độ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val orderModeText = when (detail.orderMode) {
                                "SOLITARIO" -> "Cá nhân"
                                "GRUPO" -> "Nhóm"
                                "SOLO" -> "Cá nhân"
                                "GROUP" -> "Nhóm"
                                else -> detail.orderMode
                            }
                            Text(
                                text = orderModeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mức độ dinh dưỡng",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val nutritionModeText = when (detail.nutritionMode) {
                                "CON_DATOS" -> "Theo dõi dinh dưỡng"
                                "SIN_DATOS" -> "Không theo dõi"
                                "WITH_PROFILE" -> "Theo dõi dinh dưỡng"
                                "WITHOUT_PROFILE" -> "Không theo dõi"
                                else -> detail.nutritionMode
                            }
                            Text(
                                text = nutritionModeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!detail.phoneNumber.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Số điện thoại",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = detail.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (!detail.address.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Địa chỉ nhận hàng",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = detail.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                        if (!detail.paymentMethod.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Thanh toán",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val payMethodText = when (detail.paymentMethod) {
                                    "UPON_ARRIVAL" -> "Tới nơi mới trả tiền"
                                    "QR_CODE" -> "Quét mã QR"
                                    else -> detail.paymentMethod
                                }
                                Text(
                                    text = payMethodText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            if (detail.paymentMethod == "QR_CODE") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(
                                model = "https://img.vietqr.io/image/vietcombank-1037124080-compact2.png?amount=${detail.totalAmount}&addInfo=GastroLink%20Order",
                                contentDescription = "VietQR Code",
                                modifier = Modifier
                                    .size(260.dp)
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vui lòng quét mã QR trên bằng ứng dụng ngân hàng hoặc ví điện tử để thanh toán số tiền %,d đ".format(detail.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section: Perfil nutricional (conditional)
            if (detail.profileType != null || detail.profileSummaryLines.isNotEmpty()) {
                item {
                    SectionHeader(title = "Hồ sơ dinh dưỡng")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(GastroSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            if (detail.profileSummaryLines.isNotEmpty()) {
                                detail.profileSummaryLines.forEach { line ->
                                    Text(
                                        text = "• $line",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else if (detail.profileType != null) {
                                Text(
                                    text = "Đã lưu hồ sơ (không có tóm tắt)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!detail.profileParseable) {
                                Text(
                                    text = "Đã lưu hồ sơ (không thể phân tích)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section: Participantes (conditional)
            if (detail.participants.isNotEmpty()) {
                item {
                    SectionHeader(title = "Thành viên")
                    Column(verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)) {
                        detail.participants.forEach { participant ->
                            Text(
                                text = "• ${participant.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Section: Platos header
            item {
                SectionHeader(title = "Món ăn")
            }

            // Dish items
            items(
                detail.items,
                key = { "${it.dishName}_${it.participantName}_${it.quantity}" }
            ) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GastroSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.dishName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.price > 0) {
                                val totalPrice = item.price * item.quantity
                                Text(
                                    text = "%,d đ".format(totalPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!item.participantName.isNullOrBlank()) {
                                Text(
                                    text = item.participantName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "×${item.quantity}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = GastroSpacing.sm,
                                    vertical = GastroSpacing.xs
                                )
                            )
                        }
                    }
                }
            }

            if (detail.totalAmount > 0) {
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
                                text = "Số tiền thanh toán",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "%,d đ".format(detail.totalAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Section: Totales nutricionales
            item {
                SectionHeader(title = "Tổng giá trị dinh dưỡng")
                NutritionStatGrid(
                    kcal = detail.totals.kcal,
                    proteinG = detail.totals.proteinG,
                    carbsG = detail.totals.carbsG,
                    fatG = detail.totals.fatG
                )
            }

            // Delete button + spacer
            item {
                Button(
                    onClick = { onDeleteOrder(detail.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = "Xóa đơn hàng")
                }
                Spacer(modifier = Modifier.height(GastroSpacing.xl))
            }
        }
    }
}
