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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import coil.compose.AsyncImage
import apps.visnkmr.batu.store.*
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import apps.visnkmr.batu.store.StoreApp
import apps.visnkmr.batu.store.StoreRepository
import apps.visnkmr.batu.store.startDownload
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
public fun focusablelayo(onClick: () -> Unit, content: @Composable () -> Unit) {
     val focusManager = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    
    androidx.compose.material3.Surface(
        tonalElevation = if (focused) 6.dp else 2.dp,
        shadowElevation = if (focused) 8.dp else 2.dp,
        shape = RoundedCornerShape(12.dp),
        color =  if (!focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            // .size(width = 260.dp, height = 130.dp)
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
        // Card(
        //     colors = CardDefaults.cardColors(
        //         containerColor = if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        //     ),
        //     modifier = Modifier
        //         .fillMaxSize()
        //         .onKeyEvent { event ->
        //             // Handle DPAD using Compose KeyEvent API
        //             if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        //             when (event.key) {
        //                 Key.DirectionCenter, Key.Enter -> {
        //                     onclick()
        //                     true
        //                 }
        //                 Key.DirectionDown -> { 
        //                     focusManager.moveFocus(FocusDirection.Down)
        //                     true 
        //                 }
        //                 Key.DirectionUp -> { 
        //                     focusManager.moveFocus(FocusDirection.Up)
        //                     true 
        //                 }
        //                 Key.DirectionLeft -> { 
        //                     focusManager.moveFocus(FocusDirection.Left)
        //                     true 
        //                 }
        //                 Key.DirectionRight -> { 
        //                     focusManager.moveFocus(FocusDirection.Right)
        //                     true 
        //                 }
        //                 else -> false
        //             }
        //         }
                
        //         .focusRequester(requester)
        //         .onFocusChanged { focused = it.isFocused }
        //         .focusable(),
        //         contentAlignment = Alignment.Center
        // ) {
        content()
        // }
}
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
public fun nohlfocusablelayo(onClick: () -> Unit, content: @Composable () -> Unit, hlval: Float = 1.05f) {
     val focusManager = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    
    androidx.compose.material3.Surface(
        // tonalElevation = if (focused) 6.dp else 2.dp,
        // shadowElevation = if (focused) 8.dp else 2.dp,
        // shape = RoundedCornerShape(12.dp),        
        color =  Color.Transparent,
        modifier = Modifier
            // .size(width = 260.dp, height = 130.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(true, interactionSource = interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onClick
            )
            .scale(if (focused) hlval else 1.0f)
    ) {
        // Card(
        //     colors = CardDefaults.cardColors(
        //         containerColor = if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        //     ),
        //     modifier = Modifier
        //         .fillMaxSize()
        //         .onKeyEvent { event ->
        //             // Handle DPAD using Compose KeyEvent API
        //             if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        //             when (event.key) {
        //                 Key.DirectionCenter, Key.Enter -> {
        //                     onclick()
        //                     true
        //                 }
        //                 Key.DirectionDown -> { 
        //                     focusManager.moveFocus(FocusDirection.Down)
        //                     true 
        //                 }
        //                 Key.DirectionUp -> { 
        //                     focusManager.moveFocus(FocusDirection.Up)
        //                     true 
        //                 }
        //                 Key.DirectionLeft -> { 
        //                     focusManager.moveFocus(FocusDirection.Left)
        //                     true 
        //                 }
        //                 Key.DirectionRight -> { 
        //                     focusManager.moveFocus(FocusDirection.Right)
        //                     true 
        //                 }
        //                 else -> false
        //             }
        //         }
                
        //         .focusRequester(requester)
        //         .onFocusChanged { focused = it.isFocused }
        //         .focusable(),
        //         contentAlignment = Alignment.Center
        // ) {
        content()
        // }
}
}


@Composable
fun TvStoreScreenWrapper(
    onOpenDetails: (StoreApp) -> Unit,
    onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> },
    onOpenDownloads: () -> Unit
) {
    // Simple wrapper that reuses StoreHome-like grid but arranged for TV shelves
    TvHome(onOpenDetails = onOpenDetails, onOpenApkInfo = onOpenApkInfo, onOpenDownloads = onOpenDownloads)
    // TvStoreScreen(onOpenDetails = onOpenDetails, onOpenApkInfo = onOpenApkInfo, onOpenDownloads = onOpenDownloads)
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
                title = { Text("Vishnu N K's AppStore") },
                actions = {
                    // Add Downloads button for TV UI
                ElevatedButton(onClick = onOpenDownloads) {
                Text("Downloads")
            }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(12.dp)
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            errorState.value?.let { Text("Error: $it") }

            // Simple heuristics for sections (can be refined based on tags/lastUpdated)
            val featured = appsState.value.take(2)
            val trending = appsState.value.drop(2).take(2)
            val all = appsState.value

            if(featured.isEmpty() || trending.isEmpty() || all.isEmpty()) return@Column

            if(featured.isNotEmpty()){
                SectionRow(title = "Featured", data = featured, onOpenDetails = onOpenDetails)
                Spacer(Modifier.height(24.dp))
            }
            if(trending.isNotEmpty()){
                SectionRow(title = "Trending", data = trending, onOpenDetails = onOpenDetails)
                Spacer(Modifier.height(24.dp))
            }
            if(all.isNotEmpty()){
                SectionRow(title = "All apps", data = all, onOpenDetails = onOpenDetails)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionRow(title: String, data: List<StoreApp>,onOpenDetails: (StoreApp) -> Unit,) {
    nohlfocusablelayo(onClick={},content = {Text(title, style = MaterialTheme.typography.titleMedium)})
    Spacer(Modifier.height(8.dp))
    
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(data) { app ->
            TvAppCard(app = app, onClick = { onOpenDetails(app) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TvCard(
    app: StoreApp,
    onClick: () -> Unit
) {
    // Highlight on focus; open details on click/OK
    val interaction = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
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
        Column(
            Modifier
                .padding(10.dp)
                .onKeyEvent { event ->
                    // Handle DPAD using Compose KeyEvent API
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            onClick()
                            true
                        }
                        Key.DirectionDown -> { 
                            focusManager.moveFocus(FocusDirection.Down)
                            true 
                        }
                        Key.DirectionUp -> { 
                            focusManager.moveFocus(FocusDirection.Up)
                            true 
                        }
                        Key.DirectionLeft -> { 
                            focusManager.moveFocus(FocusDirection.Left)
                            true 
                        }
                        Key.DirectionRight -> { 
                            focusManager.moveFocus(FocusDirection.Right)
                            true 
                        }
                        else -> false
                    }
                }
                .focusable()
        ) {
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
    val appsState = remember { mutableStateOf<List<apps.visnkmr.batu.store.StoreApp>>(emptyList()) }
    val errorState = remember { mutableStateOf<String?>(null) }
    var repoApp by remember { mutableStateOf<apps.visnkmr.batu.store.StoreApp?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            apps.visnkmr.batu.store.StoreRepository.loadIfNeeded()
            val app = apps.visnkmr.batu.store.StoreRepository.bySlug(slug)
            repoApp = app
        }
            .onFailure { errorState.value = it.message }
    }
    Scaffold(
        // topBar = { TopAppBar(title = { Text(repoApp?.title ?: "App") }) }
    ) { pad ->
        if (errorState.value != null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Error: ${errorState.value}")
            }
            return@Scaffold
        }
        
        val app = repoApp
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
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = app.iconUrl(),
                    contentDescription = app.title,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = buildString {
                        app.version?.let { append("Version ").append(it) }
                        app.lastUpdated?.let {
                            if (isNotEmpty()) append(" • "); append("Updated ").append(it)
                        }
                        app.download?.let {
                            if (isNotEmpty()) append(" • "); append(it).append(" downloads")
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val p = progress[app.slug] ?: 0f
                val s = status[app.slug] ?: "idle"
                Box(Modifier.size(160.dp, 56.dp), contentAlignment = Alignment.Center) {
                    when (s) {
                        "idle" -> focusablelayo(onClick = {
                            startDownload(context, app, progress, status) { file ->
                                onOpenApkInfo(app.slug, file.absolutePath)
                            }
                        }) {Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) { Text("Install") }}
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
            if (app.screenshots.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    app.screenshots.forEach { path ->
                        val url = if (path.startsWith("http")) path
                        else "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/${path.trimStart('/')}"
                        nohlfocusablelayo(onClick = {}, hlval = 1.1f, content = {AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                .size(width =360.dp,height=240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .padding(end = 10.dp),
                                contentScale = ContentScale.Inside
                            )
                        })
                    }
                // }
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) { 
            Text(app.description, style = MaterialTheme.typography.bodyLarge)
                        }
        }
    }
}

