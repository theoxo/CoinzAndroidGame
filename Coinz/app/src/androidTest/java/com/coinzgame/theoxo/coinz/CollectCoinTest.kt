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
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
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
class CollectCoinTest {

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
     * Specifically this sets the user to have a "fresh" bank account and empty wallet.
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

                val wallet = firebase.collection(email).document(WALLET_DOCUMENT)
                val walletContents = mapOf<String, Any>()

                wallet.set(walletContents)
                bank.set(bankContents)
            }
        }

        auth.signOut()
    }

    /**
     * Tests collecting a dummy coin and depositing it into the bank.
     * Also checks that the combo timer bonus feature updates the UI as expected.
     * Depositing the coin into the bank is necessary to check if it was collected appropriately.
     * This requires that a user with email "testcoincollector@test.test" and
     * password "testtest111" is registered in the authentication service.
     * It also requires a fresh install of the app, so before running the test make sure
     * the app is not already installed on the device. Running this test inside of a test suite
     * will therefore not work.
     */
    @Test
    fun collectCoinTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView6 = onView(
                allOf(withId(R.id.email),
                        isDisplayed()))
        appCompatAutoCompleteTextView6.perform(replaceText("testcoincollector@test.test"))

        val appCompatAutoCompleteTextView7 = onView(
                allOf(withId(R.id.email), withText("testcoincollector@test.test"),
                        isDisplayed()))
        appCompatAutoCompleteTextView7.perform(closeSoftKeyboard())

        val appCompatEditText = onView(
                allOf(withId(R.id.password),
                        isDisplayed()))
        appCompatEditText.perform(replaceText("testtest111"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

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

        val floatingActionButton = onView(
                allOf(withId(R.id.fab_pickup),
                        childAtPosition(
                                allOf(withId(R.id.mapFragmentConstraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragmentContainer),
                                                0)),
                                3),
                        isDisplayed()))
        floatingActionButton.perform(click())

        // Get the current MainActivity and MapFragment to collect the coin on
        var currentMainActivity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync(object: Runnable {
            override fun run() {
                currentMainActivity = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).iterator().next() as MainActivity
            }
        })

        val mapFragment: MapFragment? = currentMainActivity?.supportFragmentManager
                ?.findFragmentById(R.id.fragmentContainer) as MapFragment

        // Create a dummy marker
        val marker = Marker(MarkerOptions()
                .position(LatLng(BANK_MARKER_LATITUDE, BANK_MARKER_LONGITUDE))
                .title("Test marker"))

        // Add the dummy marker to the Marker ID -> Coin HashMap
        mapFragment?.markerIdToCoin?.put(marker.id, Coin("testId", "DOLR", 0.50))

        // Collect the coin on the UI thread
        currentMainActivity?.runOnUiThread(object: Runnable {
            override fun run() {
                mapFragment?.collectCoinFromMarker(marker)
            }
        })

        Thread.sleep(2000)

        // Make sure that combo-related UI looks as expected two seconds after collecting a coin
        val textView = onView(
                allOf(withId(R.id.comboFactorText), withText("5.0%"),
                        isDisplayed()))
        textView.check(matches(withText("5.0%")))

        val textView1 = onView(
                allOf(withId(R.id.comboTimerText), withText("28"),
                        isDisplayed()))
        textView1.check(matches(withText("28")))

        // Set the rates before starting the bank to make the test repeatable
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
                allOf(withId(R.id.chooseWalletButton), withText("My Wallet"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout2),
                                        childAtPosition(
                                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                                5)),
                                1),
                        isDisplayed()))
        appCompatButton3.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(0)
        appCompatCheckedTextView.perform(click())

        val appCompatButton4 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()))
        appCompatButton4.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView2 = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView2.check(matches(withText("Coins deposited from wallet today: 1.\nCurrent bank credit: 16.14 GOLD.")))
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
