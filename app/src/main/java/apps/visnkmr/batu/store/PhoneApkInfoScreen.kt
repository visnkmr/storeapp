package apps.visnkmr.batu.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

// imports from StoreScreens utilities
import apps.visnkmr.batu.store.apkContentUri
import apps.visnkmr.batu.store.parseApkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneApkInfoScreen(
    slug: String,
    apkPath: String,
    onBack: () -> Unit,
    context: Context
) {
    val file = remember(apkPath) { File(apkPath) }
    val info = remember(file) { parseApkInfo(context, file) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with icon and main metadata
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val iconModel = try { apkContentUri(context, file) } catch (_: Exception) { null }
                AsyncImage(
                    model = iconModel,
                    contentDescription = "APK Icon",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.packageName ?: "Unknown package", style = MaterialTheme.typography.titleMedium)
                    val subtitle = buildString {
                        info.versionName?.let { append("v").append(it) }
                        info.versionCode?.let {
                            if (isNotEmpty()) append("  •  ")
                            append("code ").append(it)
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(File(apkPath).name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ElevatedButton(onClick = {
                    // launch system installer
                    val uri = apkContentUri(context, file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Install")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sections
            InfoSection(title = "Activities", entries = info.activities)
            InfoSection(title = "Services", entries = info.services)
            InfoSection(title = "Receivers", entries = info.receivers)
            InfoSection(title = "Permissions", entries = info.permissions)
        }
    }
}

@Composable
private fun InfoSection(title: String, entries: List<String>) {
    if (entries.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEachIndexed { idx, s ->
                Text(s, style = MaterialTheme.typography.bodySmall)
                if (idx != entries.lastIndex) {
                    Divider(Modifier.padding(top = 6.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
