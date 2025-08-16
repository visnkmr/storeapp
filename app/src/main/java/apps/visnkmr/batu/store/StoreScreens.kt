package apps.visnkmr.batu.store

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File

// Data model mapping minimal needed fields
data class StoreApp(
    val slug: String,
    val title: String,
    val description: String,
    val downloadUrl: String,
    val download: String?,
    val lastUpdated: String?,
    val imageKey: String?,
    val version: String?,
    val tags: List<String>,
    val screenshots: List<String>,
    val youtube: List<String>,
    val excerpt: String?,
    val applicationId: String? = null,
    val versionCode: Int? = null,
    val repoName: String? = null,
    val repoUrl: String? = null
) {
    fun iconUrl(): String? {
        // Example images path rule: images/<key>.webp on the repo
        return if (!imageKey.isNullOrBlank()) {
            "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/images/${imageKey}.png"
        } else null
    }
}

private fun parseApps(json: String): List<StoreApp> {
    val arr = JSONArray(json)
    val out = ArrayList<StoreApp>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        out.add(
            StoreApp(
                slug = o.optString("slug"),
                title = o.optString("title"),
                description = o.optString("description"),
                downloadUrl = o.optString("downloadurl"),
                download = o.optString("download", null),
                lastUpdated = o.optString("lastupdated", null),
                imageKey = o.optString("image", null),
                version = o.optString("version", null),
                tags = o.optJSONArray("tags")?.let { jArr -> List(jArr.length()) { idx -> jArr.optString(idx) } } ?: emptyList(),
                screenshots = o.optJSONArray("screenshot")?.let { jArr -> List(jArr.length()) { idx -> jArr.optString(idx) } } ?: emptyList(),
                youtube = o.optJSONArray("youtube")?.let { jArr -> List(jArr.length()) { idx -> jArr.optString(idx) } } ?: emptyList(),
                excerpt = o.optString("excerpt", null)
            )
        )
    }
    return out
}

private suspend fun fetchAppList(): List<StoreApp> = withContext(Dispatchers.IO) {
    val url = "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/list.json"
    val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    val req = Request.Builder().url(url).get().build()
    client.newCall(req).execute().use { r ->
        if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
        val body = r.body?.string().orEmpty()
        parseApps(body)
    }
}
 
// Helper to resolve consistent apk file output path per app/version
fun resolveApkFile(context: Context, app: StoreApp): File {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks/${app.slug}/${app.version ?: "latest"}")
    return File(dir, "app.apk")
}

// Build a content uri for the apk file (for installer or displaying icon)
fun apkContentUri(context: Context, file: File): Uri {
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
}

// Minimal APK info model using PackageManager.getPackageArchiveInfo
data class ApkInfo(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val icon: android.graphics.drawable.Drawable? = null // optionally include resolved icon/banner
)

fun parseApkInfo(context: Context, apk: File): ApkInfo {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val pkgInfo = pm.getPackageArchiveInfo(
        apk.absolutePath,
        PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS
    )
    // Prepare to resolve app resources (icon/banner) directly from the APK, without FileProvider URI
    pkgInfo?.applicationInfo?.apply {
        sourceDir = apk.absolutePath
        publicSourceDir = apk.absolutePath
    }
    val vCode = try {
        if (android.os.Build.VERSION.SDK_INT >= 28) pkgInfo?.longVersionCode else pkgInfo?.versionCode?.toLong()
    } catch (_: Exception) { null }

    // Try to load a rich banner first on TV, then fallback to icon
    val iconDrawable: android.graphics.drawable.Drawable? = try {
        val appInfo = pkgInfo?.applicationInfo
        val banner = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            // loadBanner may return null if banner not provided
            appInfo?.loadBanner(pm)
        } else null
        banner ?: appInfo?.loadIcon(pm)
    } catch (_: Exception) {
        null
    }

    return ApkInfo(
        packageName = pkgInfo?.packageName,
        versionName = pkgInfo?.versionName,
        versionCode = vCode,
        permissions = pkgInfo?.requestedPermissions?.toList() ?: emptyList(),
        activities = pkgInfo?.activities?.map { it.name } ?: emptyList(),
        services = pkgInfo?.services?.map { it.name } ?: emptyList(),
        receivers = pkgInfo?.receivers?.map { it.name } ?: emptyList(),
        icon = iconDrawable
    )
}
 
// Screen listing all downloaded APK files in the app's directory with install/delete
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    context: Context,
    onBack: () -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit
) {
    val filesState = remember { mutableStateOf(listDownloadedApks(context)) }

    fun clearAll() {
        val base = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
        if (base.exists()) {
            // Recursively delete all files/dirs under apks
            base.walkBottomUp().forEach { f ->
                runCatching { if (f.isFile) f.delete() else f.delete() }
            }
        }
        filesState.value = listDownloadedApks(context)
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Downloads") },
                actions = {
                    // Clear all button in the top-right
                    ElevatedButton(onClick = { clearAll() }) {
                        Text("Clear all")
                    }
                }
            ) 
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val files = filesState.value
            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No APKs downloaded")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(files) { f ->
                        val info = parseApkInfo(context, f)
                        val slugGuess = info.packageName ?: f.nameWithoutExtension
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Prefer icon/banner loaded from PackageManager if available, else fallback to URI
                                if (info.icon != null) {
                                    // Draw Drawable via AndroidView to avoid extra image libs or extensions
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    (48 * ctx.resources.displayMetrics.density).toInt(),
                                                    (48 * ctx.resources.displayMetrics.density).toInt()
                                                )
                                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                                setImageDrawable(info.icon)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    AsyncImage(
                                        model = apkContentUri(context, f),
                                        contentDescription = info.packageName ?: f.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(info.packageName ?: f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sub = buildString {
                                        info.versionName?.let { append("v").append(it) }
                                        info.versionCode?.let { 
                                            if (isNotEmpty()) append(" • ")
                                            append("code ").append(it)
                                        }
                                    }
                                    if (sub.isNotBlank()) {
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    // Text(f.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ElevatedButton(onClick = { 
                                        // Open our in-app APK info screen with the best slug guess and real path
                                        val slug = info.packageName ?: f.nameWithoutExtension
                                        onOpenApkInfo(slug, f.absolutePath)
                                    }) { Text("Install") }
                                    ElevatedButton(onClick = { 
                                        runCatching { f.delete() }
                                        // refresh
                                        filesState.value = listDownloadedApks(context)
                                    }) { Text("🗑") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper: list all .apk files under app-managed downloads directory
fun listDownloadedApks(context: Context): List<File> {
    val base = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
    if (!base.exists()) return emptyList()
    val out = ArrayList<File>()
    base.walkTopDown().forEach { f ->
        if (f.isFile && f.extension.equals("apk", ignoreCase = true)) out.add(f)
    }
    return out.sortedByDescending { it.lastModified() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreHome(
    onOpenDetails: (StoreApp) -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val appsState = remember { mutableStateOf<List<StoreApp>>(emptyList()) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val queryState = remember { mutableStateOf("") }

    // Track download states per slug for button animation
    val progressMap = remember { mutableStateMapOf<String, Float>() }
    val statusMap = remember { mutableStateMapOf<String, String>() } // idle/downloading/downloaded/installing/installed

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
                title = { Text("AppStore") },
                actions = {
                    // Navigate to downloaded files screen
                    IconButton(onClick = onOpenDownloads) {
                        // Use a simple glyph to represent Downloads to avoid icon classpath issues
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
                .padding(horizontal = 12.dp)
        ) {
            // Update banner for visnkmr.apps.appstore
            val updateTarget = remember { findByApplicationId("visnkmr.apps.appstore") }
            UpdateBanner(target = updateTarget, height = 48.dp) {
                updateTarget?.let { onOpenDetails(it) }
            }
            androidx.compose.material3.OutlinedTextField(
                value = queryState.value,
                onValueChange = { v -> queryState.value = v },
                placeholder = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            )
            errorState.value?.let { err ->
                Text("Error: $err", color = Color(0xFFB00020))
            }
            var data=appsState.value
            data = data.filter { app ->
                app.tags?.any { tag ->
                    tag.equals("aas", ignoreCase = true) ||
                            tag.equals("gp", ignoreCase = true) ||
                            tag.equals("aos", ignoreCase = true)
                } == true
            }
            val filtered = if (queryState.value.isBlank()) data
            else data.filter {
                it.title.contains(queryState.value, ignoreCase = true) ||
                        it.tags.any { t -> t.contains(queryState.value, ignoreCase = true) }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { app ->
                    AppRow(
                        app = app,
                        progress = progressMap[app.slug] ?: 0f,
                        status = statusMap[app.slug] ?: "idle",
                        onClick = { onOpenDetails(app) },
                        onInstall = { 
                            // Phone list: after download completes, do NOT auto-open APK info screen.
                            // Status will become "downloaded" and the button will change to "Install".
                            startDownload(context, app, progressMap, statusMap) { /* no-op on list */ }
                        },
                        onOpenApkInfo = { slug, apkPath -> onOpenApkInfo(slug, apkPath) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRow(
    app: StoreApp,
    progress: Float,
    status: String,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> }

    
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = app.iconUrl(),
                contentDescription = app.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildString {
                    app.excerpt?.let {
                        append(it)
                    }
                    app.lastUpdated?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Updated ").append(it)
                    }
                    app.download?.let {
                        if (isNotEmpty()) append(" • ")
                        append(it).append(" downloads")
                    }
                }
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }

            // Google Play like button animation: idle -> circular progress with percentage -> Open
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    "idle" -> {
                        ElevatedButton(onClick = onInstall) { Text("Download") }
                    }
                    "downloading" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("${(progress * 100).toInt()}%")
                        }
                    }
                    "downloaded" -> {
                        ElevatedButton(onClick = {
                            val file = resolveApkFile(context, app)
                            onOpenApkInfo(app.slug, file.absolutePath)
                        }) { Text("Install") }
                    }
                    "installing" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Installing…")
                        }
                    }
                    "installed" -> {
                        ElevatedButton(onClick = { /* Open not implemented here, handled in details */ }) { Text("Open") }
                    }
                }
            }
        }
    }
}

private fun ensureInstallPermission(context: Context): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val can = context.packageManager.canRequestPackageInstalls()
            if (!can) {
                // Prompt to settings like Play Store does when unknown sources are off
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            can
        } else true
    } catch (_: Exception) {
        true
    }
}

public fun startDownload(
    context: Context,
    app: StoreApp,
    progressMap: MutableMap<String, Float>,
    statusMap: MutableMap<String, String>,
    onDownloaded: ((File) -> Unit)? = null
) {
    if (!ensureInstallPermission(context)) {
        // User will come back after enabling; keep UI idle for now
        return
    }
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val outFile = resolveApkFile(context, app)
    val fileDir = outFile.parentFile!!
    if (!fileDir.exists()) fileDir.mkdirs()

    val request = DownloadManager.Request(Uri.parse(app.downloadUrl))
        .setAllowedOverMetered(true)
        .setTitle(app.title)
        .setDestinationUri(Uri.fromFile(outFile))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setMimeType("application/vnd.android.package-archive")

    val id = dm.enqueue(request)
    statusMap[app.slug] = "downloading"
    // Poll progress using a coroutine tied to composition
    // Launch a background coroutine tied to a global scope since we're not in @Composable here.
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        val q = DownloadManager.Query().setFilterById(id)
        var done = false
        while (!done) {
            delay(350)
            val c: Cursor = dm.query(q) ?: continue
            c.use {
                if (!it.moveToFirst()) return@use
                val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (total > 0) {
                    progressMap[app.slug] = bytes.toFloat() / total.toFloat()
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    done = true
                    statusMap[app.slug] = "downloaded"
                } else if (status == DownloadManager.STATUS_FAILED) {
                    done = true
                    statusMap[app.slug] = "idle"
                }
            }
        }
        if (statusMap[app.slug] == "downloaded") {
            // Notify UI to open APK Info screen; do not auto-launch the installer here.
            withContext(Dispatchers.Main) { onDownloaded?.invoke(outFile) }
        }
    }
}

// Compose-safe coroutine scope helper for non-composable call-sites within Composables
/* no-op: helper removed; we don't call composable helpers from non-composable code */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetails(
    app: StoreApp,
    onBack: () -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val progressMap = remember { mutableStateMapOf<String, Float>() }
    val statusMap = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.title) },
                navigationIcon = {},
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = app.iconUrl(),
                    contentDescription = app.title,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val line = buildString {
                        app.version?.let { append("Version ").append(it) }
                        app.lastUpdated?.let {
                            if (isNotEmpty()) append(" • ")
                            append("Updated ").append(it)
                        }
                    }
                    if (line.isNotBlank()) {
                        Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Repo info / issues
                    val repoText = when {
                        !app.repoUrl.isNullOrBlank() && !app.repoName.isNullOrBlank() -> "Report issues to ${app.repoName}"
                        !app.repoName.isNullOrBlank() -> "Report issues to ${app.repoName}"
                        else -> null
                    }
                    repoText?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                    val progress = progressMap[app.slug] ?: 0f
                    val status = statusMap[app.slug] ?: "idle"
                    when (status) {
                        "idle" -> {
                            val label = rememberInstallLabel(context, app).replace("Install", "Download")
                            ElevatedButton(onClick = {
                                startDownload(context, app, progressMap, statusMap) { /* after download, UI will show 'downloaded' state */ }
                            }) { Text(label) }
                        }
                        "downloading" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp)); Text("${(progress*100).toInt()}%")
                        }
                        "downloaded" -> {
                            ElevatedButton(onClick = {
                                val file = resolveApkFile(context, app)
                                onOpenApkInfo(app.slug, file.absolutePath)
                            }) { Text("Install") }
                        }
                        "installing" -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp)); Text("Installing…")
                        }
                        "installed" -> ElevatedButton(onClick = {
                            // Try to open package if we can infer; otherwise no-op
                        }) { Text("Open") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (app.screenshots.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                ) {
                    app.screenshots.forEach { path ->
                        val url = if (path.startsWith("http")) path
                        else "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/${path.trimStart('/')}"
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .width(260.dp)
                                .height(146.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(end = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(app.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Determine whether to show Install or Update based on installed versionCode vs JSON versionCode
@Composable
private fun rememberInstallLabel(context: android.content.Context, app: StoreApp): String {
    val pm = context.packageManager
    val pkg = app.applicationId
    val remoteCode = app.versionCode
    if (pkg.isNullOrBlank() || remoteCode == null) return "Install"
    return try {
        val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        val installedCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
            pkgInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode
        }
        if (installedCode < remoteCode) "Update" else "Open"
    } catch (_: Exception) {
        "Install"
    }
}
