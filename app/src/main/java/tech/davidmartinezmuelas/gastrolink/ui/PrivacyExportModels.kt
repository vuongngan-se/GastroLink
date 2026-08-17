package tech.davidmartinezmuelas.gastrolink.ui

enum class HistoryExportFormat {
    JSON,
    CSV
}

data class HistoryExportPayload(
    val fileName: String,
    val mimeType: String,
    val content: String
)

sealed interface HistoryExportResult {
    data class Success(val payload: HistoryExportPayload) : HistoryExportResult
    data object EmptyHistory : HistoryExportResult
    data object Error : HistoryExportResult
}

data class DataWipeResult(
    val success: Boolean,
    val message: String
)
