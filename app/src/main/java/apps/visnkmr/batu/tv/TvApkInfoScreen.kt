package apps.visnkmr.batu.tv

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import apps.visnkmr.batu.store.apkContentUri
import apps.visnkmr.batu.store.parseApkInfo
import java.io.File

@Composable
fun TvApkInfoScreen(
    slug: String,
    apkPath: String,
    onBack: () -> Unit,
    context: Context
) {
    val file = File(apkPath)
    val info = parseApkInfo(context, file)

    // DPAD-friendly scroll: handle up/down keys to move the scroll manually
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        // scroll by roughly one row height
                        scrollState.dispatchRawDelta(72f)
                        true
                    }
                    Key.DirectionUp -> {
                        scrollState.dispatchRawDelta(-72f)
                        true
                    }
                    Key.PageDown -> {
                        scrollState.dispatchRawDelta(420f)
                        true
                    }
                    Key.PageUp -> {
                        scrollState.dispatchRawDelta(-420f)
                        true
                    }
                    else -> false
                }
            }
    ) {
        Row {
            AsyncImage(
                model = apkContentUri(context, file),
                contentDescription = info.packageName ?: file.name,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(info.packageName ?: slug, style = MaterialTheme.typography.titleLarge)
                val meta = buildString {
                    info.versionName?.let { append("v").append(it) }
                    info.versionCode?.let { if (isNotEmpty()) append(" • "); append("code ").append(it) }
                }
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(file.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            ElevatedButton(onClick = {
                val uri = apkContentUri(context, file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }) { Text("Install") }
        }

        Spacer(Modifier.height(20.dp))

        TvInfoSection("Activities", info.activities)
        TvInfoSection("Services", info.services)
        TvInfoSection("Receivers", info.receivers)
        TvInfoSection("Permissions", info.permissions)
    }
}

@Composable
private fun TvInfoSection(title: String, entries: List<String>) {
    if (entries.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Surface(
        tonalElevation = 2.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEachIndexed { idx, s ->
                Text(s, style = MaterialTheme.typography.bodySmall)
                if (idx != entries.lastIndex) Divider(Modifier.padding(top = 6.dp))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}
