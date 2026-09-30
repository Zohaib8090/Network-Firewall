package dev.zohaib.networkfirewall.data.update

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What the update check found. */
sealed interface UpdateResult {
    /** This app is the newest release (or newer). */
    data class UpToDate(val currentVersion: String) : UpdateResult

    /**
     * A newer release exists.
     * @property downloadUrl the APK attached to the release, or null when it has none
     */
    data class UpdateAvailable(
        val version: String,
        val releaseUrl: String,
        val downloadUrl: String?,
        val notes: String
    ) : UpdateResult

    /** The repository has no published release yet. */
    data object NoReleases : UpdateResult

    data class Failed(val reason: String) : UpdateResult
}

/** What the Settings screen shows: nothing yet, a check in progress, or its result. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Done(val result: UpdateResult) : UpdateUiState
}

/** A minimal HTTP GET, so the checker can be tested without a network. */
fun interface HttpGet {
    fun get(url: String): HttpResponse
}

data class HttpResponse(val code: Int, val body: String)

/** The real thing: a plain HTTPS request, no extra library needed. */
object UrlConnectionHttpGet : HttpGet {
    private const val TIMEOUT_MS = 10_000

    override fun get(url: String): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Network-Firewall-update-check")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return HttpResponse(code, body)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Asks GitHub for the newest published release of the repository and compares it with the installed
 * version. This is the only place the app goes online, and only when the user taps "Check for updates".
 * [check] blocks, so call it off the main thread.
 */
class UpdateChecker(
    private val currentVersion: String,
    private val owner: String = REPO_OWNER,
    private val repo: String = REPO_NAME,
    private val http: HttpGet = UrlConnectionHttpGet
) {
    fun check(): UpdateResult {
        val response = try {
            http.get("https://api.github.com/repos/$owner/$repo/releases/latest")
        } catch (e: IOException) {
            return UpdateResult.Failed("Couldn't reach GitHub. Check your internet connection and try again.")
        } catch (e: Exception) {
            return UpdateResult.Failed("Something went wrong while checking for updates.")
        }

        return when {
            // "latest" never returns drafts or pre-releases; 404 means there is no release at all
            response.code == 404 -> UpdateResult.NoReleases
            response.code == 403 || response.code == 429 ->
                UpdateResult.Failed("GitHub is limiting requests right now. Try again in a little while.")
            response.code !in 200..299 -> UpdateResult.Failed("GitHub answered with an error (${response.code}).")
            else -> parse(response.body)
        }
    }

    internal fun parse(body: String): UpdateResult {
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return UpdateResult.Failed("GitHub's answer wasn't understood.")
        }

        val tag = json.optString("tag_name").trim()
        if (tag.isEmpty()) return UpdateResult.Failed("GitHub's answer wasn't understood.")

        val latest = Versions.normalize(tag)
        if (!Versions.isNewer(latest, currentVersion)) return UpdateResult.UpToDate(currentVersion)

        // Links come from a network response, so only ever offer ones that point at github.com
        val releaseUrl = json.optString("html_url").takeIf { isGithubUrl(it) }
            ?: "https://github.com/$owner/$repo/releases"

        var downloadUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val url = asset.optString("browser_download_url")
                if (asset.optString("name").endsWith(".apk", ignoreCase = true) && isGithubUrl(url)) {
                    downloadUrl = url
                    break
                }
            }
        }

        return UpdateResult.UpdateAvailable(
            version = latest,
            releaseUrl = releaseUrl,
            downloadUrl = downloadUrl,
            notes = json.optString("body").trim()
        )
    }

    companion object {
        const val REPO_OWNER = "Zohaib8090"
        const val REPO_NAME = "Network-Firewall"

        /** Only https links on github.com are ever opened. */
        fun isGithubUrl(url: String): Boolean = url.startsWith("https://github.com/")
    }
}

/** Compares version numbers like `1.10.2` part by part, so 1.10 is newer than 1.9. */
object Versions {
    /** `v1.2.3` becomes `1.2.3`. */
    fun normalize(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

    fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0

    fun compare(a: String, b: String): Int {
        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val x = left.getOrElse(i) { 0 }
            val y = right.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    /** `1.2.0-beta+5` counts as 1.2.0: anything after a `-` or `+` is ignored. */
    private fun parts(version: String): List<Int> =
        normalize(version).substringBefore('-').substringBefore('+').split('.')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
