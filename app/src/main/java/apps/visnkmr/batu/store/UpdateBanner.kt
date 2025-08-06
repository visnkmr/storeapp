package apps.visnkmr.batu.store

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Bring in StoreApp and StoreRepository types from the same package
import apps.visnkmr.batu.store.StoreApp
import apps.visnkmr.batu.store.StoreRepository

/**
 * Simple reusable update banner. Pass a target app (e.g., the entry whose applicationId == "visnkmr.apps.appstore")
 * and we will compute if an update is available (installed versionCode < JSON versionCode).
 * If update is available, show the banner. Clicking calls onClick().
 *
 * Intended use:
 *   val target = remember { findAppByApplicationId("visnkmr.apps.appstore") }
 *   UpdateBanner(target = target, height = 48.dp) { onOpenDetails(target!!) }
 */
@Composable
fun UpdateBanner(
    target: StoreApp?,
    height: Dp,
    onClick: () -> Unit
) {
    if (target == null) return
    val context = LocalContext.current
    val show = remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        show.value = isUpdateAvailable(context.packageManager, target)
    }

    if (!show.value) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Update available for AppStore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Helper to find a StoreApp by applicationId in the in-memory StoreRepository list.
 */
fun findByApplicationId(applicationId: String): StoreApp? {
    return StoreRepository.listAll().firstOrNull { it.applicationId == applicationId }
}

/**
 * Compare installed vs remote versionCode for a StoreApp. Returns true if an update is available.
 * If either applicationId or versionCode is missing, returns false.
 */
fun isUpdateAvailable(pm: PackageManager, app: StoreApp): Boolean {
    val pkg = app.applicationId ?: return false
    val remote = app.versionCode ?: return false
    return try {
        val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        val installed = if (android.os.Build.VERSION.SDK_INT >= 28) {
            pkgInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode
        }
        installed < remote
    } catch (_: Exception) {
        // Not installed -> consider as update available (since we want to prompt user to update to latest)
        true
    }
}
