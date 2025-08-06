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
    onOpenDetails = { app -> nav.navigate("details/${app.slug}") },
    onOpenApkInfo = { s, apkPath -> nav.navigate("apk_info/${s}/${android.net.Uri.encode(apkPath)}") },
    onOpenDownloads = { nav.navigate("downloads") }
)
        }
        composable("details/{slug}") { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            apps.visnkmr.batu.store.PhoneDetailsScreen(
                slug = slug,
                onBack = { nav.popBackStack() },
                context = this@MainActivity,
                onOpenApkInfo = { s, apkPath -> nav.navigate("apk_info/${s}/${android.net.Uri.encode(apkPath)}") }
            )
        }
        // Phone APK Info screen
        composable("apk_info/{slug}/{apkPath}") { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            val apkPath = backStackEntry.arguments?.getString("apkPath") ?: ""
            apps.visnkmr.batu.store.PhoneApkInfoScreen(
                slug = slug,
                apkPath = apkPath,
                onBack = { nav.popBackStack() },
                context = this@MainActivity
            )
        }
        // Downloads screen
        composable("downloads") {
            apps.visnkmr.batu.store.DownloadsScreen(
                context = this@MainActivity,
                onBack = { nav.popBackStack() },
                onOpenApkInfo = { s, apkPath -> nav.navigate("apk_info/${s}/${android.net.Uri.encode(apkPath)}") }
            )
        }
    }
} else {
    // TV graph (shelves + details + APK info)
    NavHost(
        navController = nav,
        startDestination = "tv_home"
    ) {
        composable("tv_home") {
            apps.visnkmr.batu.tv.TvStoreScreenWrapper(
                onOpenDetails = { app -> nav.navigate("tv_details/${app.slug}") },
                onOpenApkInfo = { s, apkPath -> nav.navigate("tv_apk_info/${s}/${android.net.Uri.encode(apkPath)}") }
            ) { nav.navigate("tv_downloads") }
        }
        composable("tv_details/{slug}") { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            apps.visnkmr.batu.tv.FireTvDetailsScreen(
                slug = slug,
                onBack = { nav.popBackStack() },
                context = this@MainActivity,
                onOpenApkInfo = { s, apkPath -> nav.navigate("tv_apk_info/${s}/${android.net.Uri.encode(apkPath)}") }
            )
        }
        // TV APK Info screen
        composable("tv_apk_info/{slug}/{apkPath}") { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            val apkPath = backStackEntry.arguments?.getString("apkPath") ?: ""
            apps.visnkmr.batu.tv.TvApkInfoScreen(
                slug = slug,
                apkPath = apkPath,
                onBack = { nav.popBackStack() },
                context = this@MainActivity
            )
        }
        // TV Downloads screen reusing phone DownloadsScreen layout for now
        composable("tv_downloads") {
            apps.visnkmr.batu.store.DownloadsScreen(
                context = this@MainActivity,
                onBack = { nav.popBackStack() },
                onOpenApkInfo = { s, apkPath -> nav.navigate("tv_apk_info/${s}/${android.net.Uri.encode(apkPath)}") }
            )
        }
    }
}
                }
            }
        }
    }
}