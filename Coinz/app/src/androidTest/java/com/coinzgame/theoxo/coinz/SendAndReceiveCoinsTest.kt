package com.coinzgame.theoxo.coinz


import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
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
import java.time.Year
import java.util.*

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendAndReceiveCoinsTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")


    /**
     * Set up the database entry for the test users before running the test so that it is repeatable.
     * Specifically this sets the user "testcoincollector@test.test" and password "testtest111"
     * to have 30 coins in their wallet and enables them to send away coins in messages by
     * setting their counter for the day to 25 in their bank. It also sets the user
     * "tesetreceiver@test.test" with password "testtest111" to have a fresh bank account
     * and fresh inbox.
     * Both of these users thus need to be set up in the authentication service.
     * This test requires that this is the first time the app is being run on the system;
     * as such running it in a test suite will not work.
     */
    @Before
    fun setUpDatabase() {
        val auth = FirebaseAuth.getInstance()
        val firebase = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp instead of java.util.Date
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firebase.firestoreSettings = settings
        val email = "testcoincollector@test.test"
        val testpw = "testtest111"
        auth.signInWithEmailAndPassword(email, testpw).run {
            addOnCompleteListener {
                val wallet = firebase.collection(email).document(WALLET_DOCUMENT)
                val currencies = arrayOf("DOLR", "PENY", "QUID", "SHIL")
                val walletContents = HashMap<String, Any>()
                for (i in 0 until 30) {
                    val id = "testCoinId$i"
                    val currency = currencies[i%4]
                    val value = i + .25

                    walletContents["`$currency|$id`"] = Coin(id, currency, value).toJSON().toString()
                }

                wallet.set(walletContents)

                val bank = firebase.collection(email).document(BANK_DOCUMENT)
                val cal = Calendar.getInstance()
                val todaysDate = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}" +
                        "-${cal.get(Calendar.DAY_OF_MONTH)}"
                val bankContents = mapOf(todaysDate to 25)

                bank.set(bankContents)
            }
        }

        auth.signOut()

        val email1 = "testreceiver@test.test"

        auth.signInWithEmailAndPassword(email1, testpw).run {
            addOnCompleteListener {

                val wallet = firebase.collection(email1).document(WALLET_DOCUMENT)
                val currencies = arrayOf("DOLR", "PENY", "QUID", "SHIL")
                val walletContents = HashMap<String, Any>()
                for (i in 0 until 30) {
                    val id = "testCoinId$i"
                    val currency = currencies[i%4]
                    val value = i + .25

                    walletContents["`$currency|$id`"] = Coin(id, currency, value).toJSON().toString()
                }

                wallet.set(walletContents)

                val bank = firebase.collection(email1).document(BANK_DOCUMENT)
                val bankContents = mapOf(GOLD_FIELD_TAG to 0.0)
                val inbox = firebase.collection(email1).document(INBOX_DOCUMENT)
                val inboxContents = mapOf<String, Any>()

                bank.set(bankContents)
                inbox.set(inboxContents)
            }
        }




    }

    /**
     * This test sends two coins from one user to another through the messaging system and
     * then logs into the other user's account and deposits the coins into the bank to
     * ensure they were sent correctly.
     * It requires that users "testcoincollector@test.test" with password "testtest111"
     * and "testreceiver@test.test" with password "testtest111" are set up in the authentication
     * service. It also requires that this is the first time the app is being run on the system;
     * as such running it in a test suite will not work.
     */
    @Test
    fun sendAndReceiveCoinsTest() {
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

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!")))
        appCompatButton2.perform(scrollTo(), click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val bottomNavigationItemView = onView(
                allOf(withId(R.id.messaging_nav), withContentDescription("Messages"),
                        isDisplayed()))
        bottomNavigationItemView.perform(click())

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
                        isDisplayed()))
        appCompatEditText2.perform(replaceText("testreceiver@test.test"), closeSoftKeyboard())

        val appCompatEditText3 = onView(
                allOf(withId(R.id.message),
                        isDisplayed()))
        appCompatEditText3.perform(replaceText("Coins away!"), closeSoftKeyboard())

        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(0)
        appCompatCheckedTextView.perform(click())

        val appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(1)
        appCompatCheckedTextView2.perform(click())

        val appCompatButton4 = onView(
                allOf(withId(R.id.sendButton), withText("SEND"),
                        isDisplayed()))
        appCompatButton4.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val bottomNavigationItemView3 = onView(
                allOf(withId(R.id.account_nav), withContentDescription("Account"),
                        isDisplayed()))
        bottomNavigationItemView3.perform(click())

        val appCompatButton7 = onView(
                allOf(withId(R.id.log_out_button), withText("Log out"),
                        isDisplayed()))
        appCompatButton7.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView6 = onView(
                allOf(withId(R.id.email), withText("testcoincollector@test.test"),
                        isDisplayed()))
        appCompatAutoCompleteTextView6.perform(replaceText("testreceiver@test.test"))

        val appCompatAutoCompleteTextView7 = onView(
                allOf(withId(R.id.email), withText("testreceiver@test.test"),
                        isDisplayed()))
        appCompatAutoCompleteTextView7.perform(closeSoftKeyboard())

        val appCompatButton8 = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        isDisplayed()))
        appCompatButton8.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton9 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!")))
        appCompatButton9.perform(scrollTo(), click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        // Get the MapFragment to overwrite the rates and force the bank to open.
        var currentMainActivity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync(object: Runnable {
            override fun run() {
                currentMainActivity = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).iterator().next() as MainActivity
            }
        })

        val mapFragment: MapFragment? = currentMainActivity?.supportFragmentManager
                ?.findFragmentById(R.id.fragmentContainer) as MapFragment

        mapFragment?.rates = JSONObject("{\"SHIL\": 51.28148957923587, "
                + "\"DOLR\": 32.271807953909644, "
                + "\"QUID\": 47.650279691530336, "
                + "\"PENY\": 17.15222932298055}")
        mapFragment?.startBank()

        val appCompatButton10 = onView(
                allOf(withId(R.id.chooseInboxButton), withText("My Inbox")))
        appCompatButton10.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatCheckedTextView3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(0)
        appCompatCheckedTextView3.perform(click())

        val appCompatCheckedTextView4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(1)
        appCompatCheckedTextView4.perform(click())

        val appCompatButton11 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        isDisplayed()))
        appCompatButton11.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView.check(matches(withText("Coins deposited from wallet today: 0.\nCurrent bank credit: 403.40 GOLD.")))
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
