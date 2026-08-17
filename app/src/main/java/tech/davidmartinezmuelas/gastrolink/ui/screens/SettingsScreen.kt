package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.ui.BuildInfo
import tech.davidmartinezmuelas.gastrolink.ui.DataWipeResult
import tech.davidmartinezmuelas.gastrolink.ui.ExportShareHelper
import tech.davidmartinezmuelas.gastrolink.ui.HistoryExportFormat
import tech.davidmartinezmuelas.gastrolink.ui.HistoryExportResult
import tech.davidmartinezmuelas.gastrolink.ui.components.SectionHeader
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing

@Composable
fun SettingsScreen(
    isPremiumDemoEnabled: Boolean,
    canUseAiRecommendations: Boolean,
    nutritionMode: NutritionMode?,
    useAiRecommendations: Boolean,
    buildInfo: BuildInfo,
    isSyncing: Boolean = false,
    syncStatusMessage: String? = null,
    onUploadSync: () -> Unit = {},
    onDownloadSync: () -> Unit = {},
    onTogglePremiumDemo: (Boolean) -> Unit,
    onToggleUseAiRecommendations: (Boolean) -> Unit,
    onOpenPlans: () -> Unit,
    onExportHistory: suspend (HistoryExportFormat) -> HistoryExportResult,
    onDeleteAllData: suspend () -> DataWipeResult,
    onOpenHistory: () -> Unit,
    onNavigateStart: () -> Unit,
    onBack: () -> Unit
) {
    val canShowAiToggle = canUseAiRecommendations && nutritionMode == NutritionMode.WITH_PROFILE
    val snackbarHostState = remember { SnackbarHostState() }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showExportDialog = remember { mutableStateOf(false) }
    val showPremiumEnableDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GastroTopBar(
                title = "Cài đặt",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GastroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            // Plan card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumDemoEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
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
                            text = if (isPremiumDemoEnabled) "Premium Demo" else "Gói Free",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPremiumDemoEnabled) {
                                "Mọi tính năng đều sẵn sàng"
                            } else {
                                "Không có hồ sơ dinh dưỡng & gợi ý"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenPlans) {
                        Text(text = "Xem gói dịch vụ")
                    }
                }
            }

            // Section: Plan y acceso
            SectionHeader(title = "Gói & Quyền truy cập")
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
                            text = "Premium Demo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Kích hoạt hồ sơ dinh dưỡng, gợi ý món ăn và thống kê nâng cao. Hoàn toàn miễn phí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPremiumDemoEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPremiumEnableDialog.value = true
                            } else {
                                onTogglePremiumDemo(false)
                            }
                        },
                        modifier = Modifier.padding(start = GastroSpacing.sm)
                    )
                }
            }

            // Section: Navegación
            SectionHeader(title = "Điều hướng")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(GastroSpacing.md)) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Lịch sử & Thống kê")
                    }
                }
            }

            // Section: Recomendaciones IA (conditional)
            if (canShowAiToggle) {
                SectionHeader(title = "Gợi ý từ AI")
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
                                text = "Sử dụng AI (beta)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Các gợi ý món ăn sẽ được tạo qua máy chủ bên ngoài.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useAiRecommendations,
                            onCheckedChange = onToggleUseAiRecommendations,
                            modifier = Modifier.padding(start = GastroSpacing.sm)
                        )
                    }
                }
            }

            // Section: Cloud Sync (MySQL)
            SectionHeader(title = "Đồng bộ đám mây (MySQL) ☁️")
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
                    Text(
                        text = "Đồng bộ an toàn hồ sơ sức khỏe và lịch sử đặt món của bạn với cơ sở dữ liệu đám mây MySQL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                    ) {
                        Button(
                            onClick = onUploadSync,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            enabled = !isSyncing
                        ) {
                            Text(text = "Sao lưu ☁️")
                        }

                        OutlinedButton(
                            onClick = onDownloadSync,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            enabled = !isSyncing
                        ) {
                            Text(text = "Khôi phục 📥")
                        }
                    }

                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = syncStatusMessage ?: "Đang đồng bộ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (syncStatusMessage != null) {
                        Text(
                            text = syncStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncStatusMessage.contains("Lỗi")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Section: Privacidad
            SectionHeader(title = "Quyền riêng tư")
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
                    OutlinedButton(
                        onClick = { showExportDialog.value = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Xuất lịch sử đặt món")
                    }
                    Button(
                        onClick = { showDeleteDialog.value = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(text = "Xóa tất cả dữ liệu")
                    }
                }
            }

            // Section: Acerca de
            SectionHeader(title = "Giới thiệu")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)
                ) {
                    Text(
                        text = "Phiên bản ${buildInfo.versionName} (${buildInfo.versionCode})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Git: ${buildInfo.gitSha}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Build: ${buildInfo.buildTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(GastroSpacing.xl))
        }
    }

    if (showPremiumEnableDialog.value) {
        PremiumEnableDialog(
            onConfirm = { showPremiumEnableDialog.value = false; onTogglePremiumDemo(true) },
            onDismiss = { showPremiumEnableDialog.value = false }
        )
    }

    if (showDeleteDialog.value) {
        DeleteDataDialog(
            onConfirm = {
                showDeleteDialog.value = false
                scope.launch {
                    val result = onDeleteAllData()
                    snackbarHostState.showSnackbar(result.message)
                    if (result.success) onNavigateStart()
                }
            },
            onDismiss = { showDeleteDialog.value = false }
        )
    }

    if (showExportDialog.value) {
        ExportDialog(
            onExportJson = {
                showExportDialog.value = false
                scope.launch {
                    handleExport(HistoryExportFormat.JSON, onExportHistory, context, snackbarHostState)
                }
            },
            onExportCsv = {
                showExportDialog.value = false
                scope.launch {
                    handleExport(HistoryExportFormat.CSV, onExportHistory, context, snackbarHostState)
                }
            },
            onDismiss = { showExportDialog.value = false }
        )
    }
}

@Composable
private fun PremiumEnableDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Kích hoạt Premium Demo") },
        text = {
            Text(
                text = "Thao tác này sẽ kích hoạt chế độ Premium để trải nghiệm mọi tính năng nâng cao:\n\n" +
                    "• Cá nhân hóa hồ sơ dinh dưỡng\n" +
                    "• Gợi ý món ăn theo mục tiêu\n" +
                    "• Gợi ý bằng AI (beta)\n" +
                    "• Thống kê chuyên sâu nâng cao\n\n" +
                    "Hoàn toàn miễn phí, không yêu cầu thanh toán thực tế."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = "Kích hoạt") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Hủy") } }
    )
}

@Composable
private fun DeleteDataDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Xóa tất cả dữ liệu") },
        text = {
            Text(
                text = "Hành động này sẽ xóa vĩnh viễn:\n\n" +
                    "• Toàn bộ lịch sử đặt món\n" +
                    "• Tất cả hồ sơ dinh dưỡng đã lưu\n" +
                    "• Các cài đặt và tùy chọn khác\n\n" +
                    "Không thể hoàn tác hành động này."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Xóa vĩnh viễn", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Hủy") } }
    )
}

@Composable
private fun ExportDialog(onExportJson: () -> Unit, onExportCsv: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Xuất lịch sử") },
        text = { Text(text = "Chọn định dạng tệp để xuất") },
        confirmButton = { TextButton(onClick = onExportJson) { Text(text = "JSON") } },
        dismissButton = { TextButton(onClick = onExportCsv) { Text(text = "CSV") } }
    )
}

private suspend fun handleExport(
    format: HistoryExportFormat,
    onExportHistory: suspend (HistoryExportFormat) -> HistoryExportResult,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState
) {
    when (val exportResult = onExportHistory(format)) {
        is HistoryExportResult.Success -> {
            val shared = ExportShareHelper.shareExportFile(context, exportResult.payload)
            if (!shared) {
                snackbarHostState.showSnackbar("Không thể chia sẻ tệp")
            }
        }

        HistoryExportResult.EmptyHistory -> {
            snackbarHostState.showSnackbar("Không có lịch sử để xuất")
        }

        HistoryExportResult.Error -> {
            snackbarHostState.showSnackbar("Không thể xuất lịch sử")
        }
    }
}
