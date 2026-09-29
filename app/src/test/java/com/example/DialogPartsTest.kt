package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.model.AppRule
import com.example.ui.components.TimeField
import com.example.ui.components.TimePickerDialog
import com.example.ui.components.limitsToSave
import com.example.ui.theme.SmartNetworkGuardTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Text fields inside a dialog window never go idle under Robolectric (the auto-focused field's
 * cursor animation), so the dialogs are covered through their parts instead.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DialogPartsTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val mb = 1024L * 1024L

    @Test
    fun `saving data limits keeps the stored weekly limit`() {
        val rule = AppRule(packageName = "a", appName = "A", weeklyLimitBytes = 2000 * mb)
        assertEquals(Triple(500 * mb, 2000 * mb, 5000 * mb), limitsToSave(rule, "500", "5000"))
    }

    @Test
    fun `blank or invalid limit text means no limit`() {
        val rule = AppRule(packageName = "a", appName = "A")
        assertEquals(Triple(0L, 0L, 0L), limitsToSave(rule, "", "abc"))
    }

    @Test
    fun `time field shows the time and reports taps`() {
        var taps = 0
        composeTestRule.setContent {
            SmartNetworkGuardTheme {
                TimeField(
                    label = "From", hour = 22, minute = 5, onClick = { taps++ },
                    modifier = Modifier.testTag("time_field")
                )
            }
        }
        composeTestRule.onNodeWithText("22:05").assertExists()

        composeTestRule.onNodeWithTag("time_field").performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `time picker dialog confirms with the chosen time`() {
        var confirmed: Pair<Int, Int>? = null
        composeTestRule.setContent {
            SmartNetworkGuardTheme {
                TimePickerDialog(
                    title = "Start time", initialHour = 22, initialMinute = 30,
                    onConfirm = { h, m -> confirmed = h to m }, onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Start time").assertExists()

        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(22 to 30, confirmed)
    }

    @Test
    fun `time picker dialog cancel does not confirm`() {
        var confirmed: Pair<Int, Int>? = null
        var dismissed = false
        composeTestRule.setContent {
            SmartNetworkGuardTheme {
                TimePickerDialog(
                    title = "End time", initialHour = 6, initialMinute = 0,
                    onConfirm = { h, m -> confirmed = h to m }, onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertNull(confirmed)
        assertEquals(true, dismissed)
    }
}
