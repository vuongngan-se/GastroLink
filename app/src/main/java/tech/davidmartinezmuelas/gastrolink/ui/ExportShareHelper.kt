package tech.davidmartinezmuelas.gastrolink.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ExportShareHelper {

    fun shareExportFile(context: Context, payload: HistoryExportPayload): Boolean {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, payload.fileName)
            file.writeText(payload.content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = payload.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Chia sẻ xuất dữ liệu")
            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            if (error is ActivityNotFoundException) {
                false
            } else {
                false
            }
        }
    }
}
