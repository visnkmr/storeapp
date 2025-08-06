package apps.visnkmr.batu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.Context.UI_MODE_SERVICE
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import apps.visnkmr.batu.ui.theme.TVCalendarTheme
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import apps.visnkmr.batu.data.AppDatabase
import apps.visnkmr.batu.data.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

@SuppressLint("CustomSplashScreen")
class MainActivity : ComponentActivity() {

    private fun isTvDevice(): Boolean {
        val uiMode = (getSystemService(UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
        val isUiTv = uiMode == Configuration.UI_MODE_TYPE_TELEVISION
        val pm = packageManager
        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTelevision = pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        return isUiTv || hasLeanback || hasTelevision
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build encrypted preferences for storing the OpenRouter API key
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            this,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        setContent {
            var dark by remember { mutableStateOf(true) }

            TVCalendarTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isTv = remember { isTvDevice() }
                    val nav = rememberNavController()
                    if (!isTv) {
                        // Phone/Tablet graph
                        NavHost(
                            navController = nav,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                apps.visnkmr.batu.store.StoreHome(
                                    onOpenDetails = { app -> nav.navigate("details/${app.slug}") }
                                )
                            }
                            composable("details/{slug}") { backStackEntry ->
                                val slug = backStackEntry.arguments?.getString("slug") ?: ""
                                apps.visnkmr.batu.store.PhoneDetailsScreen(
                                    slug = slug,
                                    onBack = { nav.popBackStack() },
                                    context = this@MainActivity
                                )
                            }
                        }
                    } else {
                        // TV graph (shelves + details)
                        NavHost(
                            navController = nav,
                            startDestination = "tv_home"
                        ) {
                            composable("tv_home") {
                                apps.visnkmr.batu.tv.TvStoreScreenWrapper(
                                    onOpenDetails = { app -> nav.navigate("tv_details/${app.slug}") }
                                )
                            }
                            composable("tv_details/{slug}") { backStackEntry ->
                                val slug = backStackEntry.arguments?.getString("slug") ?: ""
                                apps.visnkmr.batu.tv.FireTvDetailsScreen(
                                    slug = slug,
                                    onBack = { nav.popBackStack() },
                                    context = this@MainActivity
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    context: Context,
    prefs: android.content.SharedPreferences,
    dark: Boolean,
    onToggleDark: () -> Unit,
    repo: ChatRepository
) {
    val scope = rememberCoroutineScope()
    var actionsExpanded by remember { mutableStateOf(false) }

    // Drawer state and API key moved into drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var apiKey by remember { mutableStateOf(prefs.getString("openrouter_api_key", "") ?: "") }

    // Conversations and selection
    val conversations by repo.conversations().collectAsState(initial = emptyList())
    var selectedConversationId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(conversations) {
        if (selectedConversationId == null) {
            // Create a conversation if none exists
            if (conversations.isEmpty()) {
                selectedConversationId = repo.newConversation()
            } else {
                selectedConversationId = conversations.first().id
            }
        }
    }

    // Messages for selected conversation
    val messagesFlow = remember(selectedConversationId) {
        if (selectedConversationId != null) repo.messages(selectedConversationId!!) else null
    }
    val messagesInDb by (messagesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    // Input and UI
    var input by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    var allModels by remember { mutableStateOf(listOf<String>()) }
    var freeModels by remember { mutableStateOf(setOf<String>()) }
    var visibleCount by remember { mutableStateOf(5) }
    var loadingModels by remember { mutableStateOf(false) }
    var modelsError by remember { mutableStateOf<String?>(null) }
    var filterFreeOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("openrouter/auto") }
    val listState = rememberLazyListState()
    // Toggle for including chat history with the current message
    var includeHistory by remember { mutableStateOf(true) }

    // Auto-scroll state: when true, keep list pinned to bottom as new messages stream in.
    var autoScroll by remember { mutableStateOf(true) }
    // Derived: are we at (or near) the bottom currently?
    val isAtBottom by remember {
        derivedStateOf {
            val lastIndex = (messagesInDb.size - 1).coerceAtLeast(0)
            // Consider "near bottom" if the last visible item index is within 1 of the last message
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= lastIndex - 1
        }
    }

    // Right side Questions popup — use AlertDialog; ensure only one overlay is active at a time
    var showQuestions by remember { mutableStateOf(false) }

    // OkHttp client
    val client = remember {
        OkHttpClient.Builder()
            // Important for streaming: don't buffer entire body
            .retryOnConnectionFailure(true)
            .build()
    }

    // Load models lazily on first open of dropdown
    fun parseFreeFlagFromName(name: String): Boolean {
        // Heuristic fallback if API doesn't provide price metadata
        val lower = name.lowercase()
        return listOf("openrouter/auto", "free", "gemma", "mistral", "llama", "qwen", "mixtral", "openhermes", "phi-3", "smollm").any { lower.contains(it) }
    }

    fun ensureModelsLoaded() {
        if (allModels.isNotEmpty() || loadingModels) return
        loadingModels = true
        modelsError = null
        scope.launch {
            try {
                // Fetch models list (use API key when available for full metadata/access)
                val reqBuilder = Request.Builder()
                    .url("https://openrouter.ai/api/v1/models")
                    .get()
                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                    // Optional but recommended per OpenRouter guidelines:
                    reqBuilder.addHeader("HTTP-Referer", "https://example.com")
                    reqBuilder.addHeader("X-Title", "Batu Chat")
                }
                val req = reqBuilder.build()
                val resp: Response = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                resp.use { r ->
                    if (!r.isSuccessful) throw IOException("HTTP ${r.code} ${r.message}")
                    val body = r.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val data = root.optJSONArray("data") ?: JSONArray()
                    val names = mutableListOf<String>()
                    val freeSet = mutableSetOf<String>()
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val idRaw = item.optString("id")
                        if (idRaw.isNullOrBlank()) {
                            continue
                        }
                        val id = idRaw
                        names.add(id)
                        // Prefer explicit pricing metadata when available
                        val pricing = item.optJSONObject("pricing")
                        val prompt = pricing?.opt("prompt")
                        val completion = pricing?.opt("completion")
                        val inputFree = prompt == null || prompt == JSONObject.NULL || (prompt is String && prompt.equals("0", true))
                        val outputFree = completion == null || completion == JSONObject.NULL || (completion is String && completion.equals("0", true))
                        val likelyFree = inputFree && outputFree
                        if (likelyFree || parseFreeFlagFromName(id)) {
                            freeSet.add(id)
                        }
                    }
                    names.sort()
                    allModels = names
                    freeModels = freeSet
                    // Reset visible count whenever we load anew
                    visibleCount = 5
                }
            } catch (e: Exception) {
                modelsError = e.message ?: "Failed to load models"
            } finally {
                loadingModels = false
            }
        }
    }

    suspend fun updateAssistantStreaming(messageId: Long, delta: String) {
        val m = messagesInDb.find { it.id == messageId } ?: return
        repo.updateMessageContent(messageId, m.content + delta)
    }

    suspend fun streamChat(conversationId: Long, prompt: String) {
        // create assistant message placeholder and capture ID for streaming updates
        val assistantId = repo.addAssistantPlaceholder(conversationId)
        withContext(Dispatchers.IO) {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val mediaType = "application/json".toMediaType()
            // Build minimal JSON; we use org.json already on Android
            val bodyJson = JSONObject().apply {
                put("model", selectedModel)
                put("stream", true)
                put("messages", JSONArray().apply {
                    // Reconstruct conversation history from DB (last 40 messages)
                    val history = messagesInDb.takeLast(40)
                    history.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                    // Append current user prompt
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://example.com")
                .addHeader("X-Title", "Batu Chat")
                .post(bodyJson.toRequestBody(mediaType))
                .build()

            val call = client.newCall(request)
            val response = call.execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code} ${resp.message}")
                }
                val source: BufferedSource = resp.body?.source()
                    ?: throw IllegalStateException("Empty body")
                // Ensure we treat as a stream
                while (true) {
                    if (!isActive) break
                    val rawLine = source.readUtf8Line() ?: break
                    val line = rawLine.trim()
                    if (line.isEmpty()) {
                        // skip blanks -- do nothing
                    } else if (line.startsWith("data:", ignoreCase = true)) {
                        // OpenRouter streams "data: {json}" lines
                        val payload = line.substringAfter("data:", "").trim()
                        if (payload == "[DONE]") {
                            break
                        }
                        try {
                            val obj = JSONObject(payload)
                            val choices = obj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0)
                                    .optJSONObject("delta")
                                    ?.optString("content") ?: ""
                                if (delta.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        updateAssistantStreaming(assistantId, delta)
                                    }
                                }
                            } else {
                                // Some providers send "message" full chunks
                                val msg = obj.optJSONObject("message")
                                val content = msg?.optString("content").orEmpty()
                                if (content.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        updateAssistantStreaming(assistantId, content)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore malformed lines
                        }
                    }
                }
            }
        }
    }

    // Primary left drawer (conversations/settings)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                Text("Conversations", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = {
                    scope.launch {
                        selectedConversationId = repo.newConversation()
                    }
                }) { Text("＋") }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                conversations.forEach { conv ->
                    NavigationDrawerItem(
                        label = { Text(conv.title) },
                        selected = conv.id == selectedConversationId,
                        onClick = { selectedConversationId = conv.id },
                        colors = NavigationDrawerItemDefaults.colors()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("sk-or-v1-...") },
                        singleLine = true,
                        label = { Text("OpenRouter API Key") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            prefs.edit().putString("openrouter_api_key", apiKey.trim()).apply()
                        }
                    ) {
                        Text("💾")
                    }
                }
                
                
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    IconButton(onClick = onToggleDark) {
                        Text(if (dark) "☀ Light" else "🌙 dark")
                    }
                }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Prevent entire UI from jumping when IME appears; we'll pad for IME only at the bottom bar.
                .padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                 // Drawer menu
                 IconButton(onClick = {
                    if (showQuestions) showQuestions = false
                    scope.launch { drawerState.open() }
                }) {
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                }
                Spacer(Modifier.width(8.dp))
                // App title centered with weight
                Text(
                    text = "Batu Chat",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                // Actions dropdown trigger at old model-button position
                Box {
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Text("🤖")
                    }
                    DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Models") },
                            onClick = {
                                actionsExpanded = false
                                ensureModelsLoaded()
                                showModels = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New chat") },
                            onClick = {
                                actionsExpanded = false
                                scope.launch { selectedConversationId = repo.newConversation() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Questions") },
                            onClick = {
                                actionsExpanded = false
                                showQuestions = false
                                scope.launch {
                                    drawerState.close()
                                    showQuestions = true
                                }
                            }
                        )
                    }
                }
               
            }
//            Spacer(Modifier.height(8.dp))
//            Divider()
            // Secondary quick-actions row removed; actions now live in the dropdown
            Spacer(Modifier.height(0.dp))

//        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            // Center: messages
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    reverseLayout = false,
                ) {
                itemsIndexed(messagesInDb) { index, msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        val bubbleColor = if (isUser) Color(0xFF007AFF) else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .background(bubbleColor, shape = MaterialTheme.shapes.medium)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(msg.content, color = textColor)
                            }
                            // Actions row
                            Row {
                                IconButton(onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("message", msg.content))
                                }) { Text("⧉") }
                                if (isUser) {
                                    IconButton(onClick = {
                                        // Resend same prompt in current conversation
                                        if (selectedConversationId != null) {
                                            scope.launch {
                                                val conv = selectedConversationId!!
                                                repo.addUserMessage(conv, msg.content)
                                                val assistantId = repo.addAssistantPlaceholder(conv)
                                                try {
                                                    streamChat(conv, msg.content)
                                                } catch (e: Exception) {
                                                    repo.updateMessageContent(assistantId, "[error] ${e.message}")
                                                }
                                            }
                                        }
                                    }) { Text("↻") }
                                    IconButton(onClick = {
                                        // Branch from this message
                                        scope.launch {
                                            val newId = repo.branchFromMessage(msg.id)
                                            if (newId > 0) {
                                                selectedConversationId = newId
                                            }
                                        }
                                    }) { Text("⎘") }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(60.dp)) }
                }

                // Floating auto-scroll toggle/scroll-to-bottom button
                // Show when user is NOT at bottom OR when autoScroll is off (to allow enabling).
                val showFab by remember { derivedStateOf { !isAtBottom || !autoScroll } }
                if (showFab) {
                    // Place at bottom-right, slightly above the input area spacing
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            if (!isAtBottom) {
                                // Jump to bottom and enable auto-scroll
                                scope.launch {
                                    val last = (messagesInDb.size - 1).coerceAtLeast(0)
                                    listState.scrollToItem(last)
                                }
                                autoScroll = true
                            } else {
                                // Toggle auto-scroll state
                                autoScroll = !autoScroll
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            if (!isAtBottom) "↓" else if (autoScroll) "✓" else "✕"
                        )
                    }
                }
            }

            // Right: Questions popup trigger + end drawer content
            // We render only a small trigger column here; the actual list is in an end-side ModalNavigationDrawer below
//            Column(
//                modifier = Modifier
//                    .width(220.dp)
//                    .fillMaxHeight()
//                    .padding(start = 8.dp)
//            ) {
//                Text("Questions", style = MaterialTheme.typography.titleMedium)
//                Spacer(Modifier.height(8.dp))
//
//            }
        }

        // Bottom input bar
        // Apply IME padding here so only this bar shifts above the keyboard, not the entire screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.ime
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Send a message") }
                )
//                OutlinedButton(onClick = {
//                    // Avoid animations when toggling dialog/drawer
//                    showQuestions = false
//                    scope.launch {
//                        drawerState.close()
//                        showQuestions = true
//                    }
//                }) { Text("Questions") }
//                Spacer(Modifier.width(8.dp))
                // Toggle icon for including chat history with this message
                IconButton(
                    onClick = { includeHistory = !includeHistory }
                ) {
                    // Simple visual: checked/unchecked box icon using Text until explicit icons are added
                    Text(if (includeHistory) "H✓" else "H")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val trimmed = input.trim()
                        val conv = selectedConversationId
                        if (trimmed.isNotEmpty() && conv != null) {
                            if (apiKey.isBlank()) {
                                scope.launch { drawerState.open() }
                                return@IconButton
                            }
                            val userPrompt = trimmed
                            input = ""
                            scope.launch {
                                try {
                                    repo.addUserMessage(conv, userPrompt)
                                    if (includeHistory) {
                                        streamChat(conv, userPrompt)
                                    } else {
                                        val assistantId = repo.addAssistantPlaceholder(conv)
                                        withContext(Dispatchers.IO) {
                                            val url = "https://openrouter.ai/api/v1/chat/completions"
                                            val mediaType = "application/json".toMediaType()
                                            val bodyJson = JSONObject().apply {
                                                put("model", selectedModel)
                                                put("stream", true)
                                                put("messages", JSONArray().apply {
                                                    put(JSONObject().apply {
                                                        put("role", "user")
                                                        put("content", userPrompt)
                                                    })
                                                })
                                            }.toString()
                                            val request = Request.Builder()
                                                .url(url)
                                                .addHeader("Authorization", "Bearer $apiKey")
                                                .addHeader("HTTP-Referer", "https://example.com")
                                                .addHeader("X-Title", "Batu Chat")
                                                .post(bodyJson.toRequestBody(mediaType))
                                                .build()
                                            val call = client.newCall(request)
                                            val response = call.execute()
                                            response.use { resp ->
                                                if (!resp.isSuccessful) {
                                                    throw IllegalStateException("HTTP ${resp.code} ${resp.message}")
                                                }
                                                val source: okio.BufferedSource = resp.body?.source()
                                                    ?: throw IllegalStateException("Empty body")
                                                while (true) {
                                                    if (!isActive) break
                                                    val rawLine = source.readUtf8Line() ?: break
                                                    val line = rawLine.trim()
                                                    if (line.isEmpty()) {
                                                        // skip
                                                    } else if (line.startsWith("data:", ignoreCase = true)) {
                                                        val payload = line.substringAfter("data:", "").trim()
                                                        if (payload == "[DONE]") break
                                                        try {
                                                            val obj = JSONObject(payload)
                                                            val choices = obj.optJSONArray("choices")
                                                            if (choices != null && choices.length() > 0) {
                                                                val delta = choices.getJSONObject(0)
                                                                    .optJSONObject("delta")
                                                                    ?.optString("content") ?: ""
                                                                if (delta.isNotEmpty()) {
                                                                    withContext(Dispatchers.Main) {
                                                                        updateAssistantStreaming(assistantId, delta)
                                                                    }
                                                                }
                                                            } else {
                                                                val msg = obj.optJSONObject("message")
                                                                val content = msg?.optString("content").orEmpty()
                                                                if (content.isNotEmpty()) {
                                                                    withContext(Dispatchers.Main) {
                                                                        updateAssistantStreaming(assistantId, content)
                                                                    }
                                                                }
                                                            }
                                                        } catch (_: Exception) {
                                                            // ignore malformed
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (ce: CancellationException) {
                                    // ignore
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }
                    },
                ) { Text("➤") }
            }
        }
    }

    // Auto-scroll behavior: when autoScroll is enabled and new messages stream in or appear,
    // keep the list pinned to the last item. Trigger on messagesInDb size changes.
    LaunchedEffect(messagesInDb.size, autoScroll) {
        if (autoScroll && messagesInDb.isNotEmpty()) {
            val last = messagesInDb.size - 1
            listState.scrollToItem(last)
        }
    }

    if (showModels) {
        AlertDialog(
            onDismissRequest = { showModels = false },
            title = { Text("Select Model") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (loadingModels) {
                        Text("Loading models…")
                        Spacer(Modifier.height(8.dp))
                    }
                    modelsError?.let { err ->
                        Text("Error: $err", color = Color(0xFFB00020))
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Search models") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterFreeOnly, onCheckedChange = { filterFreeOnly = it })
                        Text("Free models only")
                    }
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    val filtered = allModels.filter { m ->
                        val okFree = if (filterFreeOnly) freeModels.contains(m) else true
                        val okSearch = if (searchQuery.isNotBlank()) m.contains(searchQuery, ignoreCase = true) else true
                        okFree && okSearch
                    }
                    val toShow = filtered.take(visibleCount)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        items(toShow) { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                val isFree = freeModels.contains(m)
                                IconButton(onClick = {
                                    selectedModel = m
                                    showModels = false
                                }) {
                                    Text("✓")
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(m, modifier = Modifier.weight(1f))
                                if (isFree) {
                                    Spacer(Modifier.widthIn(6.dp))
                                    Text("FREE", color = Color(0xFF2E7D32))
                                }
                            }
                        }
                        item {
                            if (toShow.isEmpty()) {
                                Text("No models found")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = {
                            val filteredSize = filtered.size
                            visibleCount = (visibleCount + 5).coerceAtMost(filteredSize)
                        }) {
                            Text("＋5")
                        }
                    }
                }
            },
            confirmButton = {
                IconButton(onClick = { showModels = false }) { Text("✕") }
            }
        )
    }

    }

    // End-side drawer for "Questions in this thread"
    if (showQuestions) {
        val questionItems = remember(messagesInDb) {
            messagesInDb.mapIndexedNotNull { idx, m ->
                if (m.role == "user") Pair(idx, m) else null
            }
        }
        AlertDialog(
            onDismissRequest = { showQuestions = false },
            title = { Text("Questions in this thread") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(questionItems.size) { i ->
                            val (idx, msg) = questionItems[i]
                            IconButton(onClick = {
                                scope.launch {
                                    // Jump instantly without animation to avoid any UI effect
                                    listState.scrollToItem(idx)
                                    showQuestions = false
                                }
                            }) {
                                Text("▶")
                            }
                            Text(msg.content.take(120))
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                IconButton(onClick = { showQuestions = false }) { Text("✕") }
            }
        )
    }

    // Remove duplicated "Models dialog retained as before" block which caused duplicate Composable blocks and syntax errors
}
