package dev.zohaib.networkfirewall

import dev.zohaib.networkfirewall.data.update.HttpGet
import dev.zohaib.networkfirewall.data.update.HttpResponse
import dev.zohaib.networkfirewall.data.update.UpdateChecker
import dev.zohaib.networkfirewall.data.update.UpdateResult
import dev.zohaib.networkfirewall.data.update.Versions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.UnknownHostException

// org.json is Android's own implementation, so these run under Robolectric
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateCheckerTest {

    private fun checker(current: String = "1.0", http: HttpGet) = UpdateChecker(current, http = http)

    private fun respond(code: Int, body: String = "") = HttpGet { HttpResponse(code, body) }

    private fun release(
        tag: String = "v1.2",
        htmlUrl: String = "https://github.com/Zohaib8090/Network-Firewall/releases/tag/v1.2",
        assets: String = """[{"name":"Network-Firewall-v1.2.apk","browser_download_url":"https://github.com/Zohaib8090/Network-Firewall/releases/download/v1.2/Network-Firewall-v1.2.apk"}]""",
        notes: String = "Faster scrolling"
    ) = """{"tag_name":"$tag","html_url":"$htmlUrl","body":"$notes","assets":$assets}"""

    @Test
    fun `asks GitHub for the latest release of this repository`() {
        var asked: String? = null
        checker { url -> asked = url; HttpResponse(404, "") }.check()
        assertEquals("https://api.github.com/repos/Zohaib8090/Network-Firewall/releases/latest", asked)
    }

    @Test
    fun `no release yet`() {
        assertEquals(UpdateResult.NoReleases, checker(http = respond(404)).check())
    }

    @Test
    fun `a newer release is offered with its APK and notes`() {
        val result = checker("1.0", respond(200, release())).check()

        result as UpdateResult.UpdateAvailable
        assertEquals("1.2", result.version)
        assertEquals("https://github.com/Zohaib8090/Network-Firewall/releases/tag/v1.2", result.releaseUrl)
        assertEquals(
            "https://github.com/Zohaib8090/Network-Firewall/releases/download/v1.2/Network-Firewall-v1.2.apk",
            result.downloadUrl
        )
        assertEquals("Faster scrolling", result.notes)
    }

    @Test
    fun `the same or an older release means up to date`() {
        assertEquals(UpdateResult.UpToDate("1.2"), checker("1.2", respond(200, release(tag = "v1.2"))).check())
        assertEquals(UpdateResult.UpToDate("2.0"), checker("2.0", respond(200, release(tag = "v1.2"))).check())
    }

    @Test
    fun `a release without an APK still offers the release page`() {
        val result = checker("1.0", respond(200, release(assets = "[]"))).check()

        result as UpdateResult.UpdateAvailable
        assertNull(result.downloadUrl)
        assertTrue(result.releaseUrl.startsWith("https://github.com/"))
    }

    @Test
    fun `only APK assets are offered for download`() {
        val assets = """[{"name":"notes.txt","browser_download_url":"https://github.com/o/r/releases/download/v1.2/notes.txt"},
            {"name":"app.apk","browser_download_url":"https://github.com/o/r/releases/download/v1.2/app.apk"}]"""
        val result = checker("1.0", respond(200, release(assets = assets))).check()

        result as UpdateResult.UpdateAvailable
        assertEquals("https://github.com/o/r/releases/download/v1.2/app.apk", result.downloadUrl)
    }

    @Test
    fun `links that do not point at github are never offered`() {
        val assets = """[{"name":"app.apk","browser_download_url":"https://evil.example/app.apk"}]"""
        val body = release(htmlUrl = "https://evil.example/page", assets = assets)

        val result = checker("1.0", respond(200, body)).check()

        result as UpdateResult.UpdateAvailable
        assertNull(result.downloadUrl)
        assertEquals("https://github.com/Zohaib8090/Network-Firewall/releases", result.releaseUrl)
    }

    @Test
    fun `rate limiting is explained`() {
        for (code in listOf(403, 429)) {
            val result = checker(http = respond(code)).check() as UpdateResult.Failed
            assertTrue(result.reason, result.reason.contains("limiting"))
        }
    }

    @Test
    fun `a server error is reported with its code`() {
        val result = checker(http = respond(500)).check() as UpdateResult.Failed
        assertTrue(result.reason.contains("500"))
    }

    @Test
    fun `being offline is reported`() {
        val result = checker { throw UnknownHostException("api.github.com") }.check() as UpdateResult.Failed
        assertTrue(result.reason.contains("Couldn't reach GitHub"))
        val timeout = checker { throw IOException("timeout") }.check() as UpdateResult.Failed
        assertTrue(timeout.reason.contains("Couldn't reach GitHub"))
    }

    @Test
    fun `an unexpected error never crashes the check`() {
        val result = checker { throw IllegalStateException("boom") }.check()
        assertTrue(result is UpdateResult.Failed)
    }

    @Test
    fun `an answer that is not a release is reported`() {
        assertTrue(checker(http = respond(200, "<html>not json</html>")).check() is UpdateResult.Failed)
        assertTrue(checker(http = respond(200, """{"message":"hello"}""")).check() is UpdateResult.Failed)
    }

    // ---- version comparison

    @Test
    fun `version numbers are compared part by part`() {
        assertTrue(Versions.isNewer("1.10", "1.9"))
        assertTrue(Versions.isNewer("v2.0", "1.99.99"))
        assertTrue(Versions.isNewer("1.0.1", "1.0"))
        assertFalse(Versions.isNewer("1.0", "1.0.0"))
        assertFalse(Versions.isNewer("1.9", "1.10"))
        assertFalse(Versions.isNewer("1.0", "1.0"))
    }

    @Test
    fun `a v prefix and pre-release suffix are ignored`() {
        assertEquals("1.2.3", Versions.normalize(" v1.2.3 "))
        assertFalse(Versions.isNewer("1.2.0-beta", "1.2.0"))
        assertTrue(Versions.isNewer("1.3.0-beta+7", "1.2.0"))
    }
}
