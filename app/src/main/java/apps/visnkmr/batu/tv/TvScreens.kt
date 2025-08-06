package apps.visnkmr.batu.tv

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import apps.visnkmr.batu.store.StoreApp
import apps.visnkmr.batu.store.StoreRepository
import apps.visnkmr.batu.store.startDownload

@Composable
fun TvStoreScreenWrapper(
    onOpenDetails: (StoreApp) -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> },
    onOpenDownloads: () -> Unit
) {
    // Simple wrapper that reuses StoreHome-like grid but arranged for TV shelves
    TvHome(onOpenDetails = onOpenDetails, onOpenApkInfo = onOpenApkInfo, onOpenDownloads = onOpenDownloads)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvHome(
    onOpenDetails: (StoreApp) -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> },
    onOpenDownloads: () -> Unit
) {
    // Use the same centralized repository as phone to avoid divergence
    val appsState = remember { mutableStateOf<List<StoreApp>>(emptyList()) }
    val errorState = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            StoreRepository.loadIfNeeded()
            StoreRepository.listFiltered()
        }
            .onSuccess { appsState.value = it }
            .onFailure { errorState.value = it.message }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("AppStore (TV)") },
                actions = {
                    // Add Downloads button for TV UI
                    androidx.compose.material3.IconButton(onClick = onOpenDownloads) {
                        Text("📁")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            errorState.value?.let { Text("Error: $it") }
            // Single shelf for now: "All apps with images"
            Text("All", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                appsState.value.forEach { app ->
                    TvCard(app = app, onClick = { onOpenDetails(app) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvCard(
    app: StoreApp,
    onClick: () -> Unit
) {
    // Highlight on focus; open details on click/OK
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    androidx.compose.material3.Surface(
        tonalElevation = if (focused) 6.dp else 2.dp,
        shadowElevation = if (focused) 8.dp else 2.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(width = 260.dp, height = 180.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(true, interactionSource = interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onClick
            )
            .scale(if (focused) 1.05f else 1.0f)
    ) {
        Column(Modifier.padding(10.dp)) {
            AsyncImage(
                model = app.iconUrl(),
                contentDescription = app.title,
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                app.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireTvDetailsScreen(
    slug: String,
    onBack: () -> Unit,
    context: Context,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit
) {
    // Simple reuse of phone layout with slightly larger paddings for TV
    val repoApp = apps.visnkmr.batu.store.StoreRepository.bySlug(slug)
    Scaffold(
        topBar = { TopAppBar(title = { Text(repoApp?.title ?: "App") }) }
    ) { pad ->
        if (repoApp == null) {
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
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = repoApp.iconUrl(),
                    contentDescription = repoApp.title,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(repoApp.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = buildString {
                        repoApp.version?.let { append("Version ").append(it) }
                        repoApp.lastUpdated?.let {
                            if (isNotEmpty()) append(" • "); append("Updated ").append(it)
                        }
                        repoApp.download?.let {
                            if (isNotEmpty()) append(" • "); append(it).append(" downloads")
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val p = progress[repoApp.slug] ?: 0f
                val s = status[repoApp.slug] ?: "idle"
                Box(Modifier.size(160.dp, 56.dp), contentAlignment = Alignment.Center) {
                    when (s) {
                        "idle" -> ElevatedButton(onClick = {
                            startDownload(context, repoApp, progress, status) { file ->
                                onOpenApkInfo(repoApp.slug, file.absolutePath)
                            }
                        }) { Text("Install") }
                        "downloading" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = p.coerceIn(0f, 1f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp)); Text("${(p * 100).toInt()}%")
                        }
                        "downloaded" -> Text("Verifying…")
                        "installing" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp)); Text("Installing…")
                        }
                        "installed" -> ElevatedButton(onClick = { }) { Text("Open") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (repoApp.screenshots.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    repoApp.screenshots.forEach { path ->
                        val url = if (path.startsWith("http")) path
                        else "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/${path.trimStart('/')}"
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 360.dp, height = 200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .padding(end = 10.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(repoApp.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
