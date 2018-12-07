package com.coinzgame.theoxo.coinz


import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendAndViewMessageTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    /**
     * Tests logging in as one user, sending a message to another user, logging in as the second
     * user and viewing the message. Thereby tests the message passing system in completion.
     * For this test to run as expected two users need to be registered:
     * one with email "testcoincollector@test.test" and password "testtest111",
     * and one with email "testreceiver@test.test" and password "testtest111".
     * It also requires a fresh install of the app, so before running the test make sure
     * the app is not already installed on the device. Running this test inside of a test suite
     * will therefore not work.
     */
    @Test
    fun sendAndViewMessageTest() {

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView = onView(
                allOf(withId(R.id.email),
                        isDisplayed()))
        appCompatAutoCompleteTextView.perform(replaceText("testcoincollector@test.test"), closeSoftKeyboard())

        val appCompatEditText = onView(
                allOf(withId(R.id.password),
                        isDisplayed()))
        appCompatEditText.perform(replaceText("testtest111"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        isDisplayed()))
        appCompatButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!")))
        appCompatButton2.perform(scrollTo(), click())

        val bottomNavigationItemView = onView(
                allOf(withId(R.id.messaging_nav), withContentDescription("Messages"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_nav_bar),
                                        0),
                                0),
                        isDisplayed()))
        bottomNavigationItemView.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton3 = onView(
                allOf(withId(R.id.newMessageButton), withText("SEND NEW MESSAGE"),
                        isDisplayed()))
        appCompatButton3.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatEditText2 = onView(
                allOf(withId(R.id.targetEmail),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout),
                                        0),
                                0),
                        isDisplayed()))
        appCompatEditText2.perform(replaceText("testreceiver@test.test"), closeSoftKeyboard())

        val randomDouble = ThreadLocalRandom.current().nextDouble()
        // Random double to include in the message

        val appCompatEditText3 = onView(
                allOf(withId(R.id.message),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout2),
                                        0),
                                0),
                        isDisplayed()))
        appCompatEditText3.perform(replaceText("Test$randomDouble"), closeSoftKeyboard())

        val appCompatButton4 = onView(
                allOf(withId(R.id.sendButton), withText("SEND"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                4),
                        isDisplayed()))
        appCompatButton4.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val bottomNavigationItemView2 = onView(
                allOf(withId(R.id.account_nav), withContentDescription("Account"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_nav_bar),
                                        0),
                                2),
                        isDisplayed()))
        bottomNavigationItemView2.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton5 = onView(
                allOf(withId(R.id.log_out_button), withText("Log out"),
                        isDisplayed()))
        appCompatButton5.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView2 = onView(
                allOf(withId(R.id.email), withText("testcoincollector@test.test"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout),
                                        0),
                                0),
                        isDisplayed()))
        appCompatAutoCompleteTextView2.perform(replaceText("testreceiver@test.test"))

        val appCompatAutoCompleteTextView3 = onView(
                allOf(withId(R.id.email), withText("testreceiver@test.test"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout),
                                        0),
                                0),
                        isDisplayed()))
        appCompatAutoCompleteTextView3.perform(closeSoftKeyboard())

        val appCompatButton6 = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()))
        appCompatButton6.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton7 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3)))
        appCompatButton7.perform(scrollTo(), click())

        val bottomNavigationItemView3 = onView(
                allOf(withId(R.id.messaging_nav), withContentDescription("Messages"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_nav_bar),
                                        0),
                                0),
                        isDisplayed()))
        bottomNavigationItemView3.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.inboxListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(0)
        appCompatTextView.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView = onView(
                allOf(withId(R.id.messageTextView), withText("Test$randomDouble"),
                        isDisplayed()))
        textView.check(matches(withText("Test$randomDouble")))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
