package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import tech.davidmartinezmuelas.gastrolink.ui.ExportShareHelper
import tech.davidmartinezmuelas.gastrolink.ui.HistoryExportFormat
import tech.davidmartinezmuelas.gastrolink.ui.HistoryExportResult
import tech.davidmartinezmuelas.gastrolink.ui.OrderHistoryItemUi
import tech.davidmartinezmuelas.gastrolink.ui.components.EmptyState
import tech.davidmartinezmuelas.gastrolink.ui.components.KcalChip
import tech.davidmartinezmuelas.gastrolink.ui.components.LoadingState
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing

@Composable
fun OrderHistoryScreen(
    isLoading: Boolean,
    errorMessage: String?,
    orders: List<OrderHistoryItemUi>,
    onRefresh: () -> Unit,
    onExportHistory: suspend (HistoryExportFormat) -> HistoryExportResult,
    onViewDetails: (String) -> Unit,
    onOpenStats: () -> Unit,
    onBack: () -> Unit
) {
    val showExportDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GastroTopBar(
                title = "Lịch sử đặt món",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { showExportDialog.value = true }) { Text(text = "Xuất tệp") }
                    TextButton(onClick = onOpenStats) { Text(text = "Thống kê") }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingState(
                    message = "Đang tải lịch sử...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            !errorMessage.isNullOrBlank() -> {
                EmptyState(
                    icon = Icons.Filled.Warning,
                    title = "Lỗi khi tải",
                    subtitle = errorMessage,
                    actionLabel = "Thử lại",
                    onAction = onRefresh,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            orders.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Star,
                    title = "Chưa có đơn hàng",
                    subtitle = "Hãy xác nhận đơn hàng đầu tiên của bạn để xem tại đây",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                ) {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.padding(GastroSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = formatDate(order.createdAt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = order.branchName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    KcalChip(kcal = order.totalCalories)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${order.dishCount} món ăn",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onViewDetails(order.id) }) {
                                        Text(text = "Xem chi tiết")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog.value) {
        HistoryExportDialog(
            onExportJson = {
                showExportDialog.value = false
                scope.launch {
                    handleHistoryExport(HistoryExportFormat.JSON, onExportHistory, context, snackbarHostState)
                }
            },
            onExportCsv = {
                showExportDialog.value = false
                scope.launch {
                    handleHistoryExport(HistoryExportFormat.CSV, onExportHistory, context, snackbarHostState)
                }
            },
            onDismiss = { showExportDialog.value = false }
        )
    }
}

@Composable
private fun HistoryExportDialog(
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Xuất lịch sử") },
        text = { Text(text = "Chọn định dạng tệp để xuất") },
        confirmButton = { TextButton(onClick = onExportJson) { Text(text = "JSON") } },
        dismissButton = { TextButton(onClick = onExportCsv) { Text(text = "CSV") } }
    )
}

private suspend fun handleHistoryExport(
    format: HistoryExportFormat,
    onExportHistory: suspend (HistoryExportFormat) -> HistoryExportResult,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState
) {
    when (val result = onExportHistory(format)) {
        is HistoryExportResult.Success -> {
            val shared = ExportShareHelper.shareExportFile(context, result.payload)
            if (!shared) snackbarHostState.showSnackbar("Không thể chia sẻ tệp")
        }
        HistoryExportResult.EmptyHistory -> snackbarHostState.showSnackbar("Không có lịch sử để xuất")
        HistoryExportResult.Error -> snackbarHostState.showSnackbar("Không thể xuất lịch sử")
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
