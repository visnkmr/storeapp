package apps.visnkmr.batu.tv

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import apps.visnkmr.batu.store.apkContentUri
import apps.visnkmr.batu.store.parseApkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvApkInfoScreen(
    slug: String,
    apkPath: String,
    onBack: () -> Unit,
    context: Context
) {
    val file = remember(apkPath) { File(apkPath) }
    val info = remember(file) { parseApkInfo(context, file) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("APK Info (TV)") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = try { apkContentUri(context, file) } catch (_: Exception) { null },
                    contentDescription = "APK Icon",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.packageName ?: "Unknown package", style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val meta = buildString {
                        info.versionName?.let { append("v").append(it) }
                        info.versionCode?.let {
                            if (isNotEmpty()) append(" • "); append("code ").append(it)
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun TvInfoSection(title: String, entries: List<String>) {
    if (entries.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
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
