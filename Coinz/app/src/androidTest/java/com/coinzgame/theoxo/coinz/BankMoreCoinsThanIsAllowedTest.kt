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
import org.hamcrest.core.IsInstanceOf
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BankMoreCoinsThanIsAllowedTest {

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
     * Specifically this sets the user to have a "fresh" bank account and 30 valid coins in
     * their wallet.
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
                val wallet = firebase.collection(email).document(WALLET_DOCUMENT)
                val bank = firebase.collection(email).document(BANK_DOCUMENT)
                val currencies = arrayOf("DOLR", "PENY", "QUID", "SHIL")
                val walletContents = HashMap<String, Any>()
                for (i in 0 until 30) {
                    val id = "testCoinId$i"
                    val currency = currencies[i%4]
                    val value = i + .25

                    walletContents["`$currency|$id`"] = Coin(id, currency, value).toJSON().toString()
                }

                val bankContents = mapOf(GOLD_FIELD_TAG to 0.0)

                wallet.set(walletContents)
                bank.set(bankContents)
            }
        }

        auth.signOut()

    }

    @Test
    fun bankMoreCoinsThanIsAllowedTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatAutoCompleteTextView2 = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout),
                                        0),
                                0),
                        isDisplayed()))
        appCompatAutoCompleteTextView2.perform(replaceText("testcoincollector@test.test"))

        val appCompatAutoCompleteTextView3 = onView(
                allOf(withId(R.id.email), withText("testcoincollector@test.test"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout),
                                        0),
                                0),
                        isDisplayed()))
        appCompatAutoCompleteTextView3.perform(closeSoftKeyboard())

        val appCompatEditText = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.textInputLayout2),
                                        0),
                                0),
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

        // Get the current MainActivity and MapFragment to override the exchange rates
        var currentMainActivity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync(object: Runnable {
            override fun run() {
                currentMainActivity = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).iterator().next() as MainActivity
            }
        })

        val mapFragment: MapFragment? = currentMainActivity?.supportFragmentManager
                ?.findFragmentById(R.id.fragmentContainer) as MapFragment

        // Set the rates to make the test repeatable
        mapFragment?.rates = JSONObject("{\"SHIL\": 51.28148957923587, "
                + "\"DOLR\": 32.271807953909644, "
                + "\"QUID\": 47.650279691530336, "
                + "\"PENY\": 17.15222932298055}")

        // Start the bank
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

        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(0)
        appCompatCheckedTextView.perform(click())

        val appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(1)
        appCompatCheckedTextView2.perform(click())

        val appCompatCheckedTextView3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(2)
        appCompatCheckedTextView3.perform(click())

        val appCompatCheckedTextView4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(4)
        appCompatCheckedTextView4.perform(click())

        val appCompatCheckedTextView5 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(3)
        appCompatCheckedTextView5.perform(click())

        val appCompatCheckedTextView6 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(5)
        appCompatCheckedTextView6.perform(click())

        val appCompatCheckedTextView7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(7)
        appCompatCheckedTextView7.perform(click())

        val appCompatCheckedTextView8 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(8)
        appCompatCheckedTextView8.perform(click())

        val appCompatCheckedTextView9 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(6)
        appCompatCheckedTextView9.perform(click())

        val appCompatCheckedTextView10 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(9)
        appCompatCheckedTextView10.perform(click())

        val appCompatCheckedTextView11 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(10)
        appCompatCheckedTextView11.perform(click())

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

        val appCompatCheckedTextView12 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(0)
        appCompatCheckedTextView12.perform(click())

        val appCompatCheckedTextView13 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(1)
        appCompatCheckedTextView13.perform(click())

        val appCompatCheckedTextView14 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(3)
        appCompatCheckedTextView14.perform(click())

        val appCompatCheckedTextView15 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(4)
        appCompatCheckedTextView15.perform(click())

        val appCompatCheckedTextView16 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(2)
        appCompatCheckedTextView16.perform(click())

        val appCompatCheckedTextView17 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(5)
        appCompatCheckedTextView17.perform(click())

        val appCompatCheckedTextView18 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(6)
        appCompatCheckedTextView18.perform(click())

        val appCompatCheckedTextView19 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(7)
        appCompatCheckedTextView19.perform(click())

        val appCompatCheckedTextView20 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(8)
        appCompatCheckedTextView20.perform(click())

        val appCompatCheckedTextView21 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(8)
        appCompatCheckedTextView21.perform(click())

        val appCompatCheckedTextView22 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(9)
        appCompatCheckedTextView22.perform(click())

        val appCompatButton5 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()))
        appCompatButton5.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatCheckedTextView23 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(0)
        appCompatCheckedTextView23.perform(click())

        val appCompatCheckedTextView24 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(1)
        appCompatCheckedTextView24.perform(click())

        val appCompatCheckedTextView25 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(2)
        appCompatCheckedTextView25.perform(click())

        val appCompatCheckedTextView26 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(3)
        appCompatCheckedTextView26.perform(click())

        val appCompatCheckedTextView27 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(4)
        appCompatCheckedTextView27.perform(click())

        val appCompatCheckedTextView28 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
                .atPosition(5)
        appCompatCheckedTextView28.perform(click())

        val appCompatButton6 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()))
        appCompatButton6.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView = onView(
                allOf(IsInstanceOf.instanceOf(android.widget.TextView::class.java), withText("Your banking is out of control!"),
                        isDisplayed()))
        textView.check(matches(withText("Your banking is out of control!")))

        val textView2 = onView(
                allOf(withId(android.R.id.message), withText("It looks like you're trying to deposit 6 coins from your wallet, however you can only deposit 5 due to the maximum of 25 deposited coins from the wallet per day."),
                        isDisplayed()))
        textView2.check(matches(withText("It looks like you're trying to deposit 6 coins from your wallet, however you can only deposit 5 due to the maximum of 25 deposited coins from the wallet per day.")))

        val button = onView(
                allOf(withId(android.R.id.button1),
                        isDisplayed()))
        button.check(matches(isDisplayed()))

        val appCompatButton7 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!")))
        appCompatButton7.perform(scrollTo(), click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView3 = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView3.check(matches(withText("Coins deposited from wallet today: 20.\nCurrent bank credit: 14044.12 GOLD.")))

        val appCompatCheckedTextView29 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(0)
        appCompatCheckedTextView29.perform(click())

        val appCompatCheckedTextView30 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(1)
        appCompatCheckedTextView30.perform(click())

        val appCompatCheckedTextView31 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(2)
        appCompatCheckedTextView31.perform(click())

        val appCompatCheckedTextView32 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(3)
        appCompatCheckedTextView32.perform(click())

        val appCompatCheckedTextView33 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(4)
        appCompatCheckedTextView33.perform(click())

        val appCompatButton8 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()))
        appCompatButton8.perform(click())

        val appCompatCheckedTextView34 = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView)))
                .atPosition(0)
        appCompatCheckedTextView34.perform(click())

        val appCompatButton9 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        isDisplayed()))
        appCompatButton9.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView4 = onView(
                allOf(IsInstanceOf.instanceOf(android.widget.TextView::class.java), withText("Your banking is out of control!"),
                        isDisplayed()))
        textView4.check(matches(withText("Your banking is out of control!")))

        val textView5 = onView(
                allOf(withId(android.R.id.message), withText("It looks like you're trying to deposit 1 coins from your wallet, however you can only deposit 0 due to the maximum of 25 deposited coins from the wallet per day."),
                        isDisplayed()))
        textView5.check(matches(withText("It looks like you're trying to deposit 1 coins from your wallet, however you can only deposit 0 due to the maximum of 25 deposited coins from the wallet per day.")))

        val button2 = onView(
                allOf(withId(android.R.id.button1),
                        isDisplayed()))
        button2.check(matches(isDisplayed()))

        val appCompatButton10 = onView(
                allOf(withId(android.R.id.button1), withText("Got it!"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3)))
        appCompatButton10.perform(scrollTo(), click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView6 = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView6.check(matches(withText("Coins deposited from wallet today: 25.\nCurrent bank credit: 15558.43 GOLD.")))
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
