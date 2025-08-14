package apps.visnkmr.batu.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apps.visnkmr.batu.store.StoreApp
import apps.visnkmr.batu.store.StoreRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.scale
// import apps.visnkmr.batu.tv.TvCard
/**
 * A large-screen Fire TV–style store activity built with Jetpack Compose and DPAD focus.
 * Layout:
 * - Header TopAppBar
 * - Vertically scrolling sections (e.g., "Featured", "Trending", "All Apps")
 * - Each section is a LazyRow of large hero cards that respond to DPAD focus
 */
// class FireTvStoreActivity : ComponentActivity() {
//     @OptIn(ExperimentalMaterial3Api::class)
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         setContent {
//             TvStoreScreen()
//         }
//     }
// }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TvStoreScreen( onOpenDetails: (StoreApp) -> Unit,
onOpenApkInfo: (slug: String, apkPath: String) -> Unit = { _, _ -> },
onOpenDownloads: () -> Unit) {
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
            TopAppBar(title = { Text("Appstore • TV") })
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            
            errorState.value?.let { Text("Error: $it") }

            // Simple heuristics for sections (can be refined based on tags/lastUpdated)
            val featured = appsState.value.take(2)
            val trending = appsState.value.drop(2).take(2)
            val all = appsState.value

            if (featured.isNotEmpty()) {
                SectionRow(title = "Featured", data = featured,onOpenDetails = { onOpenDetails(it) })
                Spacer(Modifier.height(18.dp))
            }
            if (trending.isNotEmpty()) {
                SectionRow(title = "Trending", data = trending,onOpenDetails = { onOpenDetails(it) })
                Spacer(Modifier.height(18.dp))
            }
            if (all.isNotEmpty()) {
                SectionRow(title = "All Apps", data = all, onOpenDetails = { onOpenDetails(it) })
            } 
        }
    }
}

@Composable
private fun SectionRow(title: String, data: List<StoreApp>,onOpenDetails: (StoreApp) -> Unit,) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(8.dp))

    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(data) { app ->
            TvAppCard(app = app,onClick = { onOpenDetails(app) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TvAppCard(app: StoreApp,onClick: () -> Unit) {
    // focus handling
    val focusManager = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    androidx.compose.material3.Surface(
        tonalElevation = if (focused) 6.dp else 2.dp,
        shadowElevation = if (focused) 8.dp else 2.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(width = 260.dp, height = 180.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(true, interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .scale(if (focused) 1.05f else 1.0f)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .size(width = 320.dp, height = 180.dp)
                .onKeyEvent { event ->
                    // Handle DPAD using Compose KeyEvent API. Compose versions differ; use native action where available.
                    val native = try { event.nativeKeyEvent } catch (_: Throwable) { null }
                    val isDown = native?.action == android.view.KeyEvent.ACTION_DOWN
                    if (!isDown) return@onKeyEvent false
                    val pressedKey = try { event.key } catch (_: Throwable) { null }
                    when (pressedKey) {
                        Key.DirectionCenter, Key.Enter -> true
                        Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                        Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                        Key.DirectionLeft -> { focusManager.moveFocus(FocusDirection.Left); true }
                        Key.DirectionRight -> { focusManager.moveFocus(FocusDirection.Right); true }
                        else -> false
                    }
                }
                .focusRequester(requester)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    AsyncImage(
                        model = app.iconUrl(),
                        contentDescription = app.title,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Column {
                        Text(app.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        // val sub = buildString {
                        //     app.version?.let { append("v").append(it) }
                        //     app.lastUpdated?.let {
                        //         if (isNotEmpty()) append(" • ")
                        //         append("Updated ").append(it)
                        //     }
                        // }
                        // if (sub.isNotBlank()) {
                        //     Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // }
                    }
                    // Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    //     ElevatedButton(onClick = {
                    //         // TODO: Launch install flow (reuse phone/tablet logic by extracting helper to shared module/file if desired)
                    //     }) { Text("Install") }
                    //     ElevatedButton(onClick = {
                    //         // TODO: Open details screen for TV
                    //     }) { Text("Details") }
                    // }
                }
            }
        }
    }
}

private suspend fun fetchAppListTv(): List<StoreApp> = withContext(Dispatchers.IO) {
    val url = "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/list.json"
    val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    val req = Request.Builder().url(url).get().build()
    client.newCall(req).execute().use { r ->
        if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
        val body = r.body?.string().orEmpty()
        parseAppsTv(body)
    }
}

private fun parseAppsTv(json: String): List<StoreApp> {
    val arr = JSONArray(json)
    val list = ArrayList<StoreApp>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        list.add(
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
    return list
}
