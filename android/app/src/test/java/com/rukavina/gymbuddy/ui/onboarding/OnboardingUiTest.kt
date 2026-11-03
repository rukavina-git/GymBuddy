package com.rukavina.gymbuddy.ui.onboarding

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rukavina.gymbuddy.data.sync.GuestMergeReceipt
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.ui.auth.SignInContent
import com.rukavina.gymbuddy.ui.auth.SignInMode
import com.rukavina.gymbuddy.ui.auth.SignInUiState
import com.rukavina.gymbuddy.ui.guest.GuestMergeDialogs
import com.rukavina.gymbuddy.ui.guest.SessionExpiredBanner
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Welcome, setup and sign-in screens (Plan 7 step 04), driven by plain state. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OnboardingUiTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun `welcome routes to setup and to sign-in`() {
        val taps = mutableListOf<String>()
        rule.setContent {
            GymBuddyTheme { WelcomeScreen(onGetStarted = { taps += "setup" }, onHaveAccount = { taps += "signIn" }) }
        }
        rule.onNodeWithText("Get started").performClick()
        rule.onNodeWithText("I have an account").performClick()
        assertEquals(listOf("setup", "signIn"), taps)
    }

    @Test
    fun `tapping an answer chooses it, and the chosen row reads as selected`() {
        var state by mutableStateOf(OnboardingUiState())
        val chosen = mutableListOf<ExperienceLevel>()
        rule.setContent {
            GymBuddyTheme {
                SetupContent(
                    state = state, onBack = {}, onSkip = {},
                    onExperience = { chosen += it; state = state.copy(experience = Answer(it, true)) },
                    onGoal = {}, onDays = {}, onPlan = {}, onFinish = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Step 1 of 4").assertIsDisplayed()
        rule.onNodeWithText("Some experience").performClick()
        assertEquals(listOf(ExperienceLevel.SOME), chosen)
        rule.onNode(hasClickAction() and hasText("Some experience", substring = true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun `goal step offers get stronger, skip and back`() {
        val taps = mutableListOf<String>()
        rule.setContent {
            GymBuddyTheme {
                SetupContent(
                    state = OnboardingUiState(step = SetupStep.GOAL),
                    onBack = { taps += "back" }, onSkip = { taps += "skip" },
                    onExperience = {}, onGoal = { taps += it.name }, onDays = {}, onPlan = {}, onFinish = {},
                )
            }
        }
        rule.onNodeWithText("Get stronger").performClick()
        rule.onNodeWithText("Skip").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf(FitnessGoal.GET_STRONGER.name, "skip", "back"), taps)
    }

    @Test
    fun `ready shows the guest tip and lets go`() {
        var went = false
        rule.setContent { GymBuddyTheme { ReadyContent(signedIn = false, saving = false, onFinish = { went = true }) } }
        rule.onNodeWithText("Your workouts are saved on this phone. Back them up from the You tab.").assertIsDisplayed()
        rule.onNodeWithText("Let's go").performClick()
        assertTrue(went)
    }

    @Test
    fun `create account shows what moves and why the box is needed`() {
        rule.setContent {
            GymBuddyTheme {
                SignInContent(
                    state = SignInUiState(mode = SignInMode.CREATE, counts = LocalDataCounts(14, 3, 2), consentMissing = true),
                    onBack = {}, onGoogle = {}, onEmail = {}, onPassword = {}, onToggleShow = {},
                    onConsent = {}, onWhy = {}, onSubmit = {}, onForgot = {}, onRetry = {},
                )
            }
        }
        rule.onNodeWithText("Your 14 workouts, 3 templates and 2 exercises move into the account. Nothing on this phone is removed.")
            .assertIsDisplayed()
        rule.onNodeWithText("Tick this box to back up your workouts. Without it, keep using GymBuddy on this phone.")
            .assertIsDisplayed()
    }

    @Test
    fun `sign-in error and offline retry`() {
        var retried = false
        rule.setContent {
            GymBuddyTheme {
                SignInContent(
                    state = SignInUiState(error = com.rukavina.gymbuddy.ui.auth.FormError.WRONG_CREDENTIALS, offline = true),
                    onBack = {}, onGoogle = {}, onEmail = {}, onPassword = {}, onToggleShow = {},
                    onConsent = {}, onWhy = {}, onSubmit = {}, onForgot = {}, onRetry = { retried = true },
                    snackbar = SnackbarHostState(),
                )
            }
        }
        // Below the fold on Robolectric's small screen: present, merged into the password field.
        rule.onNodeWithText("Email or password is wrong. Try again or reset your password.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun `session expired banner signs in`() {
        var signIn = false
        rule.setContent { GymBuddyTheme { SessionExpiredBanner(onSignIn = { signIn = true }) } }
        rule.onNodeWithText("Backup paused. Sign in again to keep backing up.").assertIsDisplayed()
        rule.onNodeWithText("Sign in").performClick()
        assertTrue(signIn)
    }

    @Test
    fun `merge question offers add or stay signed out, receipt is a snackbar`() {
        var state by mutableStateOf<GuestMergeState>(
            GuestMergeState.Asking("uid", "a@example.com", 47, LocalDataCounts(5, 1, 2))
        )
        val taps = mutableListOf<String>()
        val snackbar = SnackbarHostState()
        rule.setContent {
            GymBuddyTheme {
                GuestMergeDialogs(
                    state = state, snackbar = snackbar,
                    onAdd = { taps += "add" }, onCancel = { taps += "stay" }, onDismiss = { taps += "dismiss" },
                )
            }
        }
        rule.onNodeWithText("Add and sign in").performClick()
        rule.onNodeWithText("Stay signed out").performClick()
        state = GuestMergeState.Done(GuestMergeReceipt(LocalDataCounts(5, 0, 0), keptAccountProfile = true))
        rule.waitForIdle()
        assertEquals(listOf("add", "stay", "dismiss"), taps)
        assertEquals(
            "Added 5 workouts from this phone. Kept your account's profile.",
            snackbar.currentSnackbarData?.visuals?.message,
        )
    }
}
