package apps.visnkmr.batu.store

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
private fun installLabel(context: Context, app: StoreApp): String {
    val pkg = app.applicationId
    val remote = app.versionCode
    if (pkg.isNullOrBlank() || remote == null) return "Install"
    return try {
        val pm = context.packageManager
        val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        val installed = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
        if (installed < remote) "Update" else "Open"
    } catch (_: Exception) {
        "Install"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDetailsScreen(
    slug: String,
    onBack: () -> Unit,
    context: Context
) {
    val app = StoreRepository.bySlug(slug)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.title ?: "App") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { pad ->
        if (app == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("App not found")
            }
            return@Scaffold
        }

        val progress = remember { mutableStateMapOf<String, Float>() }
        val status = remember { mutableStateMapOf<String, String>() }

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Hero
            Row(Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = app.iconUrl(),
                    contentDescription = app.title,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = buildString {
                        app.version?.let { append("Version ").append(it) }
                        app.lastUpdated?.let {
                            if (isNotEmpty()) append(" • ")
                            append("Updated ").append(it)
                        }
                        app.download?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it).append(" downloads")
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val repoText = when {
                        !app.repoName.isNullOrBlank() -> "Report issues to ${app.repoName}"
                        else -> null
                    }
                    repoText?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Install CTA
                val p = progress[app.slug] ?: 0f
                val s = status[app.slug] ?: "idle"
                Box(Modifier.size(120.dp, 48.dp), contentAlignment = Alignment.Center) {
                    when (s) {
                        "idle" -> ElevatedButton(onClick = { startDownload(context, app, progress, status) }) { Text(installLabel(context, app)) }
                        "downloading" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = p.coerceIn(0f, 1f), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp)); Text("${(p * 100).toInt()}%")
                        }
                        "downloaded" -> Text("Verifying…")
                        "installing" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp)); Text("Installing…")
                        }
                        "installed" -> ElevatedButton(onClick = { }) { Text("Open") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Screenshots
            if (app.screenshots.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    app.screenshots.forEach { path ->
                        val url = if (path.startsWith("http")) path
                        else "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/${path.trimStart('/')}"
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 280.dp, height = 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(end = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Description
            Text(app.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
