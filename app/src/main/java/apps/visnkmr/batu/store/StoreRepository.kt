package apps.visnkmr.batu.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object StoreRepository {
    @Volatile
    private var loaded: Boolean = false
    private val client by lazy {
        OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    }

    private val apps = mutableListOf<StoreApp>()
    private val bySlug = mutableMapOf<String, StoreApp>()

    suspend fun loadIfNeeded() {
        if (loaded) return
        val list = fetch()
        apps.clear()
        apps.addAll(list)
        bySlug.clear()
        for (a in list) {
            bySlug[a.slug] = a
        }
        loaded = true
    }

    fun listAll(): List<StoreApp> = apps.toList()

    // Filter to entries that have an image to display as icon/banner
    fun listFiltered(): List<StoreApp> = apps.filter { !it.imageKey.isNullOrBlank() }

    fun bySlug(slug: String): StoreApp? = bySlug[slug]

    private suspend fun fetch(): List<StoreApp> = withContext(Dispatchers.IO) {
        val url = "https://cdn.jsdelivr.net/gh/visnkmr/appstore@main/list.json"
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
            val body = r.body?.string().orEmpty()
            parseApps(body)
        }
    }

    // Reuse the same parser logic as StoreScreens
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
                    excerpt = o.optString("excerpt", null),
                    applicationId = o.optString("applicationID", null),
                    versionCode = if (o.has("versionCode")) o.optInt("versionCode") else null,
                    repoName = o.optString("reponame", null),
                    repoUrl = o.optString("repourl", null),
                    browseUrl = o.optString("browseurl", null)
                )
            )
        }
        return out
    }
}
