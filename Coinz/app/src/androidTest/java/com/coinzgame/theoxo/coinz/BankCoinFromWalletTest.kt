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
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import android.support.test.runner.lifecycle.Stage
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.support.test.InstrumentationRegistry.getInstrumentation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.json.JSONObject
import org.junit.Before

@LargeTest
@RunWith(AndroidJUnit4::class)
class BankCoinFromWalletTest {

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

    /**
     * This tests logging in, starting the bank, and depositing a coin from the wallet.
     * Checks that the bank credit and counter is updated appropriately and that the coin is
     * removed from the ListView. Then restarts the [BankActivity] and makes sure the coin is still
     * not in the ListView.
     * This requires that a user with email "testcoincollector@test.test" and
     * password "testtest111" is registered in the authentication service.
     * It also requires a fresh install of the app, so before running the test make sure
     * the app is not already installed on the device. Running this test inside of a test suite
     * will therefore not work.
     */
    @Test
    fun bankSingleCoinTest() {

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
                allOf(withId(android.R.id.button1), withText("Got it!")))
        appCompatButton2.perform(scrollTo(), click())

        var currentMainActivity: MainActivity? = null
        getInstrumentation().runOnMainSync(object: Runnable {
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

        Thread.sleep(7000)

        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.coinsListView),
                        childAtPosition(
                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                2)))
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

        val textView = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView.check(matches(withText("Coins deposited from wallet today: 1.\nCurrent bank credit: 501.70 GOLD.")))

        val checkedTextView = onView(
                allOf(withId(R.id.checkedListItemText),
                        childAtPosition(
                                allOf(withId(R.id.coinsListView)),
                                0),
                        isDisplayed()))
        checkedTextView.check(matches(isDisplayed()))

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
                        isDisplayed()))
        appCompatButton5.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val textView1 = onView(
                allOf(withId(R.id.upperTextView),
                        isDisplayed()))
        textView1.check(matches(withText("Coins deposited from wallet today: 1.\nCurrent bank credit: 501.70 GOLD.")))

        val checkedTextView1 = onView(
                allOf(withId(R.id.checkedListItemText),
                        childAtPosition(
                                allOf(withId(R.id.coinsListView)),
                                0),
                        isDisplayed()))
        checkedTextView1.check(matches(isDisplayed()))
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
