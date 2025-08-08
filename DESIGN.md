# Batu Store App – Design Document and Function Structure

## Overview

Batu is a Kotlin/Jetpack Compose Android application that provides a lightweight, unified “store” UX for phones/tablets and Android TV. It fetches an app catalog (JSON) from a remote repo, presents list/detail screens, downloads APKs to app-scoped storage, surfaces APK metadata (icon/banner, version, permissions, components), and launches installation via system Package Installer. The TV experience uses a shelf/card layout and DPAD-friendly navigation.

Key goals:
- Single codebase with phone and TV flows.
- Compose-first UI. Material 3 for phone and TV.
- Minimal permissions and storage scope (app’s external files dir).
- Robust APK parsing without requiring content URIs for icon/banner (uses PackageManager against APK path).
- Non-silent install compliant with Android security policy; optionally MDM/privileged paths for enterprise.

---

## Architecture

### Layers

1) UI (Compose)
- Phone screens:
  - StoreHome: catalog, search, update banner, per-app download/Install transition.
  - PhoneDetailsScreen: richer app info and Download/Install actions.
  - PhoneApkInfoScreen: metadata from APK (components, permissions), Install action.
  - DownloadsScreen (shared): list APKs stored locally, per-item action bar and Clear All.
- TV screens:
  - TvStoreScreenWrapper/TvHome: shelf-like store home for TV, DPAD-friendly.
  - FireTvDetailsScreen: larger detail layout, Download/Install flow adapted to TV.
  - TvApkInfoScreen: scrollable with DPAD, APK metadata and Install action.
  - TV Downloads route reuses shared DownloadsScreen.

2) Data/Repository
- StoreRepository: loads/filters app list from a hosted JSON; caching and simple query support.
- AppDatabase/ChatRepository (chat feature present in MainActivity’s ChatScreen; ancillary to Store).

3) System/Platform Services
- DownloadManager: downloads APKs to app-scoped storage under getExternalFilesDir(DIRECTORY_DOWNLOADS)/apks/<slug>/<version|latest>/app.apk.
- PackageManager/PackageInstaller:
  - parseApkInfo uses PackageManager.getPackageArchiveInfo to read APK metadata and load icons/banners by setting applicationInfo.sourceDir/publicSourceDir.
  - Installation: launches ACTION_VIEW with content URI and flags to trigger system installer (non-silent).
- FileProvider: generates content URIs for APK files when needed.

---

## Navigation Graph

- Phone:
  - "home" → StoreHome
  - "details/{slug}" → PhoneDetailsScreen
  - "apk_info/{slug}/{apkPath}" → PhoneApkInfoScreen
  - "downloads" → DownloadsScreen

- TV:
  - "tv_home" → TvStoreScreenWrapper/TvHome
  - "tv_details/{slug}" → FireTvDetailsScreen
  - "tv_apk_info/{slug}/{apkPath}" → TvApkInfoScreen
  - "tv_downloads" → DownloadsScreen (shared)

MainActivity decides the graph based on device form factor (UiMode/feature detection).

---

## Key UX Flows

1) Browse Catalog
- Loads JSON from https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/list.json via OkHttp.
- StoreHome displays app cards with icon, title, description, tags, and dynamic button state:
  - idle → Download → downloading(progress %) → downloaded → Install.
- Search filters by title/tags.

2) Download APK
- Uses DownloadManager.
- Writes to app-scoped external storage under “apks/slug/version”.
- Status maps progress/state in Compose (progressMap/statusMap) for reactive UI.

3) Install APK
- Non-silent install using system Package Installer.
- ACTION_VIEW intent with content URI and read grant flags.
- From StoreHome’s per-app action or from APK Info screen.

4) Inspect APK
- parseApkInfo reads:
  - packageName, versionName, versionCode
  - permissions, activities, services, receivers
  - icon/banner Drawable via PackageManager:
    - Set applicationInfo.sourceDir/publicSourceDir = apkPath
    - If API ≥ 20: loadBanner(pm); fallback to loadIcon(pm)
- UI renders the Drawable using AndroidView ImageView; otherwise uses AsyncImage on FileProvider URI.

5) Manage Downloads
- DownloadsScreen lists all downloaded APKs found under the app-managed “apks” directory (walkTopDown).
- Per-item: Install and Delete.
- Clear All action in top app bar deletes all APKs (walkBottomUp) and refreshes state.

---

## Device Support

- Phone/Tablet: Material 3 Compose screens with touch-first UI.
- Android TV:
  - TvHome shelves with DPAD navigation.
  - TvApkInfoScreen supports DPAD scrolling via onKeyEvent and verticalScroll.
  - TV Downloads navigates via dedicated route and reuses shared list.

---

## Permissions and Security

- Request install unknown apps (PackageManager.canRequestPackageInstalls) flow:
  - If not allowed, deep link to Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES for this package.
- Storage: Only app’s scoped external files dir used; no broad storage permissions.
- Installation cannot be silent unless the app is device-owner or privileged; the app adheres to standard consent flow.

---

## Error Handling and Resilience

- Network: OkHttp with retryOnConnectionFailure(true).
- JSON: org.json with null-safe optXYZ.
- Download: polls DownloadManager for STATUS_SUCCESSFUL/FAILED, updates UI state accordingly.
- Icon/Metadata: exceptions while reading APK or loading Drawable are caught, gracefully degraded to URI-based AsyncImage or text.

---

## Files and Responsibilities

- app/src/main/java/apps/visnkmr/batu/MainActivity.kt
  - Entrypoint. Determines TV or phone graph, sets Compose content theme, NavHost for all routes.
  - Also includes ChatScreen (ancillary feature) using ChatRepository/AppDatabase.

- app/src/main/java/apps/visnkmr/batu/store/StoreScreens.kt
  - Data model StoreApp and JSON parsing.
  - resolveApkFile, apkContentUri helpers.
  - ApkInfo data model and parseApkInfo implementation (PackageManager against APK path + icon/banner load).
  - DownloadsScreen: list with install/delete, Clear All action in top-right.
  - StoreHome: search, update banner, list with download/install flow.
  - AppDetails: detailed view, download/install.
  - startDownload: DownloadManager logic with progress polling coroutine.

- app/src/main/java/apps/visnkmr/batu/store/PhoneDetailsScreen.kt
  - Phone detail screen with richer layout (separate file for modularity).

- app/src/main/java/apps/visnkmr/batu/store/PhoneApkInfoScreen.kt
  - Phone APK Info screen that prefers PackageManager Drawable icon with AndroidView fallback to AsyncImage.

- app/src/main/java/apps/visnkmr/batu/tv/TvScreens.kt
  - TvStoreScreenWrapper, TvHome (shelf list), FireTvDetailsScreen (larger UI).
  - TV top app bar includes Downloads action.

- app/src/main/java/apps/visnkmr/batu/tv/TvApkInfoScreen.kt
  - TV APK Info screen with DPAD-friendly vertical scrolling using onKeyEvent and rememberScrollState.

- app/src/main/java/apps/visnkmr/batu/store/StoreRepository.kt
  - Loads and filters StoreApp list (caching semantics).

- app/src/main/java/apps/visnkmr/batu/store/UpdateBanner.kt
  - Displays inline banner for app updates (e.g., “visnkmr.apps.appstore”).

- app/src/main/java/apps/visnkmr/batu/data/*
  - AppDatabase, ChatRepository (ancillary; not critical to store flows).

---

## Function Structure (Selected)

### MainActivity.kt
- fun isTvDevice(): Boolean
- override fun onCreate(savedInstanceState: Bundle?)
  - setContent { TVCalendarTheme { NavHost(...) { ... } } }
- Composable ChatScreen(...)

### StoreScreens.kt (core store utilities & screens)
- data class StoreApp(...)
- private fun parseApps(json: String): List<StoreApp>
- private suspend fun fetchAppList(): List<StoreApp>
- fun resolveApkFile(context, app): File
- fun apkContentUri(context, file): Uri
- data class ApkInfo(...)
- fun parseApkInfo(context, apk: File): ApkInfo
- @Composable fun DownloadsScreen(context, onBack, onOpenApkInfo)
  - fun clearAll()
  - LazyColumn items: per-item Card with icon, metadata, Install/Delete.
- fun listDownloadedApks(context): List<File>
- @Composable fun StoreHome(onOpenDetails, onOpenApkInfo, onOpenDownloads)
  - UpdateBanner, search, filtered list
  - AppRow(...) with state machine for Download → Install
- @Composable fun AppRow(app, progress, status, onClick, onInstall, onOpenApkInfo)
- private fun ensureInstallPermission(context): Boolean
- fun startDownload(context, app, progressMap, statusMap, onDownloaded)

### PhoneApkInfoScreen.kt
- @Composable fun PhoneApkInfoScreen(slug, apkPath, onBack, context)
  - Header (icon/banner via PackageManager Drawable or AsyncImage fallback)
  - Sections: Activities, Services, Receivers, Permissions
- private @Composable fun InfoSection(title, entries)

### TvScreens.kt
- @Composable fun TvStoreScreenWrapper(onOpenDetails, onOpenApkInfo, onOpenDownloads)
- @Composable fun TvHome(onOpenDetails, onOpenApkInfo, onOpenDownloads)
- @Composable fun FireTvDetailsScreen(slug, onBack, context, onOpenApkInfo)

### TvApkInfoScreen.kt
- @Composable fun TvApkInfoScreen(slug, apkPath, onBack, context)
  - rememberScrollState + onKeyEvent for DPAD scrolling
  - Header (AsyncImage or could be enhanced to PackageManager Drawable similarly)
  - TvInfoSection(title, entries)

---

## Non-Goals / Limitations

- Silent installs for non-privileged apps (security restricted by Android).
- Full-blown package session management for split APKs (can be added with PackageInstaller sessions if needed).
- Broad device file scanning (restricted to app-managed “apks” folder).

---

## Future Enhancements

- PackageInstaller.Session for installing splits and keeping users in-app with a single consent dialog.
- Richer error/reporting UI for DownloadManager failures.
- Search facets by tag/category and offline caching of JSON/catalog.
- TV: enhance focus indicators (scale/shadow), initial focus placement, DPAD shortcuts (e.g., long-press to details).
- Optional MDM/device-owner support for managed deployments.
- Signature verification and checksum validation post-download before install.
- Background worker to cleanup stale APKs or versions.

---

## Build & Dependencies

- Gradle Kotlin DSL
- Kotlin + Jetpack Compose (Material 3)
- OkHttp for networking
- org.json for parsing
- Coil (AsyncImage) for image loading
- FileProvider for secure file URIs

---

## Testing Notes

- Validate on API levels 24–34, especially PackageManager.getPackageArchiveInfo behavior changes and install flows.
- TV: verify DPAD scroll on TvApkInfoScreen and list navigation in DownloadsScreen.
- DownloadManager: test metered vs Wi-Fi, failures, resuming.
- Permissions: toggling “Install unknown apps” for the app and user journey to Settings.


##JSON List expected fields

slug - String: Unique identifier for the app
title - String: App display name
description - String: Full app description
downloadurl - String: Direct download URL for the APK file
download - String: Download count display text (e.g., "1.2K downloads")
lastupdated - String: Last update date/time text
image - String: Image key for the app icon (used to construct icon URL)
version - String: Version name/number
excerpt - String: Short description/summary for list view
tags - Array of Strings: Categories/tags for filtering and search
screenshot - Array of Strings: Screenshot image paths/URLs
youtube - Array of Strings: YouTube video URLs/IDs
applicationId - String: Android package name (defaults to null)
versionCode - Int: Android version code (defaults to null)
repoName - String: Repository name for issue reporting (defaults to null)
repoUrl - String: Repository URL (defaults to null)