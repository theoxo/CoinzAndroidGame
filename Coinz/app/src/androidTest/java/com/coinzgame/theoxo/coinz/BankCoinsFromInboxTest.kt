package com.coinzgame.theoxo.coinz


import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import android.support.test.runner.lifecycle.Stage
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BankCoinsFromInboxTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    /**
     * Set up the database entry for the test user before running the test so that it is repeatable.
     * Specifically this sets the user to have a "fresh" bank account and a single message
     * in their inbox.
     * This requires that a user with email "testcoincollector@test.test" and
     * password "testtest111" is registered in the authentication service.
     */
    @Before
    fun setUpDatabase() {
        val auth = FirebaseAuth.getInstance()
        val email = "testcoincollector@test.test"
        val testpw = "testtest111"
        auth.signInWithEmailAndPassword(email, testpw).run {
            addOnCompleteListener {
                val firebase = FirebaseFirestore.getInstance()
                // Use com.google.firebase.Timestamp instead of java.util.Date
                val settings = FirebaseFirestoreSettings.Builder()
                        .setTimestampsInSnapshotsEnabled(true)
                        .build()
                firebase.firestoreSettings = settings
                val bank = firebase.collection(email).document(BANK_DOCUMENT)
                val bankContents = mapOf(GOLD_FIELD_TAG to 0.0)

                val inbox = firebase.collection(email).document(INBOX_DOCUMENT)
                val inboxContents = mapOf("`6-12-2018 18:45:8|testcoincollector@test:test`"
                        to "{\"SenderEmail\":\"testcoincollector@test.test\",\"Timestamp\":\"6-12-2018 18:45:8\",\"MessageText\":\"Hey how are you?\",\"MessageAttachments\":[{\"currency\":\"PENY\",\"id\":\"DOLR|6-12-2018 18:45:80\",\"value\":0.25}, {\"currency\":\"QUID\",\"id\":\"DOLR|6-12-2018 18:45:81\",\"value\":12.25}, {\"currency\":\"SHIL\",\"id\":\"DOLR|6-12-2018 18:45:82\",\"value\":16.25}, {\"currency\":\"DOLR\",\"id\":\"DOLR|6-12-2018 18:45:83\",\"value\":4.25}, {\"currency\":\"DOLR\",\"id\":\"DOLR|6-12-2018 18:45:84\",\"value\":8.25}]}")

                inbox.set(inboxContents)
                bank.set(bankContents)
            }
        }

        auth.signOut()
    }

    /**
     * This tests logging in, starting the bank, and depositing several coins from the user's inbox.
     * Checks that the bank credit is updated appropriately, that the counter does not change
     * (since the coin is not coming from the user's wallet), and that the coin is
     * removed from the ListView. Then restarts the [BankActivity] and makes sure the coin is still
     * not in the ListView.
     * This requires that a user with email "testcoincollector@test.test" and
     * password "testtest111" is registered in the authentication service.
     * It also requires a fresh install of the app, so before running the test make sure
     * the app is not already installed on the device. Running this test inside of a test suite
     * will therefore not work.
     */
    @Test
    fun bankCoinsFromInboxTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView4 = onView(
                allOf(withId(R.id.email),
                        isDisplayed()))
        appCompatAutoCompleteTextView4.perform(replaceText("testcoincollector@test.test"))

        val appCompatAutoCompleteTextView5 = onView(
                allOf(withId(R.id.email), withText("testcoincollector@test.test"),
                        isDisplayed()))
        appCompatAutoCompleteTextView5.perform(closeSoftKeyboard())

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
                allOf(withId(android.R.id.button1), withText("Got it!"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3)))
        appCompatButton2.perform(scrollTo(), click())

        // Get the currently active MainActivity and MapFragment so that we can override the
        // rates and start the bank (espresso does not record marker clicks).
        var currentMainActivity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync(object: Runnable {
            override fun run() {
                currentMainActivity = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).iterator().next() as MainActivity
            }
        })

        val mapFragment: MapFragment? = currentMainActivity?.supportFragmentManager
                ?.findFragmentById(R.id.fragmentContainer) as MapFragment

        // Override the rates to make the results repeatable
        mapFragment?.rates = JSONObject("{\"SHIL\": 51.28148957923587, "
                + "\"DOLR\": 32.271807953909644, "
                + "\"QUID\": 47.650279691530336, "
                + "\"PENY\": 17.15222932298055}")

        // Start the BankActivity
        mapFragment?.startBank()

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton3 = onView(
                allOf(withId(R.id.chooseInboxButton), withText("My Inbox"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout2),
                                        childAtPosition(
                                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                                5)),
                                0),
                        isDisplayed()))
        appCompatButton3.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatCheckedTextView3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(2)
        appCompatCheckedTextView3.perform(click())

        val appCompatCheckedTextView4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(0)
        appCompatCheckedTextView4.perform(click())

        val appCompatButton4 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        isDisplayed()))
        appCompatButton4.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView.check(matches(withText("Coins deposited from wallet today: 0.\nCurrent bank credit: 1099.57 GOLD.")))

        pressBack()

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        mapFragment?.startBank()

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton5 = onView(
                allOf(withId(R.id.chooseWalletButton), withText("My Wallet"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout2),
                                        childAtPosition(
                                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                                5)),
                                1),
                        isDisplayed()))
        appCompatButton5.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView2 = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView2.check(matches(withText("Coins deposited from wallet today: 0.\nCurrent bank credit: 1099.57 GOLD.")))
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
