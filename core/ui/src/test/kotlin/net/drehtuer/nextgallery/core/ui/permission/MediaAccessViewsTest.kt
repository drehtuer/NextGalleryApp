package net.drehtuer.nextgallery.core.ui.permission

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaAccessViewsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mediaAccessRequest_allowClicked_requestsAccess() {
        var requests = 0
        composeRule.setContent { MediaAccessRequest(onRequestAccess = { requests++ }, onOpenSettings = {}) }

        composeRule.onNodeWithText("Allow access").performClick()

        assertEquals(1, requests)
    }

    @Test
    fun mediaAccessRequest_openSettingsClicked_opensSettings() {
        var opened = 0
        composeRule.setContent { MediaAccessRequest(onRequestAccess = {}, onOpenSettings = { opened++ }) }

        composeRule.onNodeWithText("Open app settings").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun limitedAccessBanner_selectMoreClicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { LimitedAccessBanner(onSelectMore = { clicks++ }) }

        composeRule.onNodeWithText("Select more").performClick()

        assertEquals(1, clicks)
    }
}
