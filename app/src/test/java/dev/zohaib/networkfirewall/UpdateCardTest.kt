package dev.zohaib.networkfirewall

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.zohaib.networkfirewall.data.update.UpdateResult
import dev.zohaib.networkfirewall.data.update.UpdateUiState
import dev.zohaib.networkfirewall.ui.components.UpdateCard
import dev.zohaib.networkfirewall.ui.theme.SmartNetworkGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UpdateCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val apk = "https://github.com/o/r/releases/download/v1.2/app.apk"
    private val page = "https://github.com/o/r/releases/tag/v1.2"

    private var checks = 0
    private val opened = mutableListOf<String>()

    private fun show(state: UpdateUiState) {
        composeTestRule.setContent {
            SmartNetworkGuardTheme {
                UpdateCard(
                    currentVersion = "1.0",
                    state = state,
                    onCheck = { checks++ },
                    onOpenUrl = { opened.add(it) }
                )
            }
        }
    }

    @Test
    fun `shows the installed version and a button, with no result yet`() {
        show(UpdateUiState.Idle)

        composeTestRule.onNodeWithText("Installed version 1.0").assertExists()
        composeTestRule.onNodeWithTag("btn_check_updates").assertIsEnabled()
        composeTestRule.onNodeWithTag("update_result").assertDoesNotExist()
    }

    @Test
    fun `tapping the button starts a check`() {
        show(UpdateUiState.Idle)

        composeTestRule.onNodeWithText("Check for updates").performClick()

        assertEquals(1, checks)
    }

    @Test
    fun `the button is disabled while a check is running`() {
        show(UpdateUiState.Checking)

        composeTestRule.onNodeWithTag("btn_check_updates").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Checking…").assertExists()
    }

    @Test
    fun `up to date`() {
        show(UpdateUiState.Done(UpdateResult.UpToDate("1.0")))

        composeTestRule.onNodeWithText("You're on the latest version (1.0).").assertExists()
    }

    @Test
    fun `a new version offers a download and the release page`() {
        show(UpdateUiState.Done(UpdateResult.UpdateAvailable("1.2", page, apk, "Faster scrolling")))

        composeTestRule.onNodeWithText("Version 1.2 is available").assertExists()
        composeTestRule.onNodeWithText("Faster scrolling").assertExists()

        composeTestRule.onNodeWithTag("btn_download_update").performClick()
        composeTestRule.onNodeWithTag("btn_release_page").performClick()

        assertEquals(listOf(apk, page), opened)
    }

    @Test
    fun `a release without an APK offers only the release page`() {
        show(UpdateUiState.Done(UpdateResult.UpdateAvailable("1.2", page, null, "")))

        composeTestRule.onNodeWithTag("btn_release_page").assertDoesNotExist()
        composeTestRule.onNodeWithText("Open release").performClick()

        assertEquals(listOf(page), opened)
    }

    @Test
    fun `no release published`() {
        show(UpdateUiState.Done(UpdateResult.NoReleases))

        composeTestRule.onNodeWithText("No release has been published yet.").assertExists()
    }

    @Test
    fun `a failure is explained and the button can be tapped again`() {
        show(UpdateUiState.Done(UpdateResult.Failed("Couldn't reach GitHub. Check your internet connection and try again.")))

        composeTestRule.onNodeWithText("Couldn't reach GitHub. Check your internet connection and try again.").assertExists()
        composeTestRule.onNodeWithTag("btn_check_updates").assertIsEnabled()
    }
}
