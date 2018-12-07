package com.coinzgame.theoxo.coinz

import android.content.*
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.coinzgame.theoxo.coinz.R.id.home_nav
import com.google.firebase.firestore.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import java.util.*

/**
 * The app's main Activity.
 * Handles switching between and containing the three main fragments,
 * [MapFragment], [InboxFragment], and [AccountFragment].
 * Also stores references to the user's email and firestore documents
 * for easy access by the fragments.
 */
class MainActivity :
        AppCompatActivity(),
        PermissionsListener,
        BottomNavigationView.OnNavigationItemSelectedListener
{

    private val tag = "MainActivity"

    // PermissionsManages to request permissions with if needed
    private lateinit var permissionsManager: PermissionsManager

    // Locally saved data tracking
    internal var currentDate: String? = null // FORMAT YYYY-MM-DD
    private var lastDownloadDate: String? = null
    internal var cachedMap: String? = null
    internal var ancientCoins = ArrayList<Feature>()

    // Firebase Firestore database references
    private var firestore:  FirebaseFirestore? = null
    internal var firestoreWallet: DocumentReference? = null
    internal var firestoreInbox: DocumentReference? = null
    internal var currentUserEmail: String? = null

    // The three main fragments which the app operates with
    private var mapFragment: MapFragment = MapFragment()
    private var inboxFragment: InboxFragment = InboxFragment()
    private var accountFragment: AccountFragment = AccountFragment()

    /**
     * Sets up the local Firestore references and greets the user if they are new.
     * Also sets up the Mapbox instance used for [MapFragment].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the Mapbox instance using our given API key
        Mapbox.getInstance(this, MAPBOX_KEY)

        // Get the data which was passed onto us from the previous LoginActivity
        currentUserEmail = intent?.getStringExtra(USER_EMAIL)
        val firstRunOfApp = intent?.getBooleanExtra(FIRST_TIME_RUNNING, false)
        Log.d(tag, "[onCreate] Received user email $currentUserEmail")
        Log.d(tag, "[onCreate] Received $FIRST_TIME_RUNNING $firstRunOfApp")

        if (firstRunOfApp != null && firstRunOfApp) {
            // This is the first time the user is playing the game.
            // Show them a little dialog explaining the basic concepts
            alert {
                message = ("Coinz is a social, location-based game. In order to play, you need to "
                        + "have an active internet connection, and your location service on and "
                        + "set to high accuracy mode.\n\nUse the floating action buttons to "
                        + "switch between inspecting coins and picking them up when you "
                        + "click on them.\nPicking a coin up starts a combo (displayed in the top "
                        + "right) -- utilize this to maximize your income!\n\n"
                        + "You can then exchange the coins you've collected "
                        + "for gold by visiting the bank, and send spare change to your friends\n"
                        + "(the coins in your wallet will be considered spare change if you have "
                        + "deposited 25 coins today).")
                title = "Hello there... Looks like you're new!"
                positiveButton("Got it!"){
                    // Mark that it is no longer the first time this app is running.
                    val storedPrefs = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                    val editor = storedPrefs.edit()
                    editor.putBoolean(FIRST_TIME_RUNNING, false)
                    editor.apply()
                }
            }.show()
        }

        // Set up click events for bottom nav bar
        bottom_nav_bar.setOnNavigationItemSelectedListener(this)

        // Set up Firestore
        firestore = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp instead of java.util.Date
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        // Copy the user email field for guaranteed thread safety before
        // setting up the firestore references
        val emailTag = currentUserEmail
        if (emailTag != null) {
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)
            firestoreInbox = firestore?.collection(emailTag)?.document(INBOX_DOCUMENT)
        } else {
            Log.w(tag, "[onCreate] emailTag is null")
        }
    }

    /**
     * Fetches the preferences stored on the device and invokes [checkLocationPermission].
     */
    override fun onStart() {
        super.onStart()

        // Default navigation bar item checked should be home
        bottom_nav_bar.selectedItemId = home_nav

        // Restore preferences
        val storedPrefs = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        lastDownloadDate = storedPrefs.getString(LAST_DOWNLOAD_DATE, null)
        Log.d(tag, "[onStart] Fetched lastDownloadDate: $lastDownloadDate")

        // Need to get date in onStart() because app may have been left running overnight
        val year: String = Calendar.getInstance().get(Calendar.YEAR).toString()
        // Add one to the month as it is 0-indexed
        var month: String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        var day: String = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        if (year != "2018" && year != "2019") {
            Log.e(tag, "Unsupported date")
        }
        if (month.length < 2) {
            // Pad to 0M format
            month = "0$month"
        }
        if (day.length < 2) {
            // Pad to 0D format
            day = "0$day"
        }

        currentDate = "$year/$month/$day"
        Log.d(tag, "[onStart] Today's date: $currentDate")

        if (currentDate == lastDownloadDate) {
            Log.d(tag, "[onStart] Dates match, fetching cached map")
            cachedMap = storedPrefs.getString(SAVED_MAP_JSON, null)
            if (cachedMap == null) {
                Log.w(tag, "[onStart] Dates matched but fetched cachedMap is null! "
                                + "Map will be downloaded.")
            } else {
                Log.d(tag, "[onStart] Fetched cachedMap: ${cachedMap?.take(25)}")
            }
        }

        // Also get the current ancient coins and save them for MapFragment
        val ancientShilCoinString = storedPrefs.getString(ANCIENT_SHIL, null)
        if (ancientShilCoinString != null && ancientShilCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient shil coin saved")
            ancientCoins.add(Feature.fromJson(ancientShilCoinString))
        }

        val ancientQuidCoinString = storedPrefs.getString(ANCIENT_QUID, null)
        if (ancientQuidCoinString != null  && ancientQuidCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient quid coin saved")
            ancientCoins.add(Feature.fromJson(ancientQuidCoinString))
        }

        val ancientDolrCoinString = storedPrefs.getString(ANCIENT_DOLR, null)
        if (ancientDolrCoinString != null  && ancientDolrCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient dolr coin saved")
            ancientCoins.add(Feature.fromJson(ancientDolrCoinString))
        }

        val ancientPenyCoinString = storedPrefs.getString(ANCIENT_PENY, null)
        if (ancientPenyCoinString != null  && ancientPenyCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient peny coin saved")
            ancientCoins.add(Feature.fromJson(ancientPenyCoinString))
        }

        // Finally ensure we have been given the necessary permission to get the user's location
        checkLocationPermission()
    }

    /**
     * Saves the user preferences if needed.
     * The user preferences are stored on the device only if [lastDownloadDate] does not match
     * [currentDate].
     */
    override fun onStop() {
        super.onStop()

        if (lastDownloadDate == currentDate) {
            Log.d(tag, "[onStop] Not storing date or map")
        } else {
            // Store preferences
            val settings: SharedPreferences = getSharedPreferences(PREFERENCES_FILE,
                    Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = settings.edit()

            Log.d(tag, "[onStop] Storing lastDownloadDate as currentDate: $currentDate")
            editor.putString(LAST_DOWNLOAD_DATE, currentDate)

            val sneakpeak: String? = cachedMap?.take(25)
            Log.d(tag, "[onStop] Storing cachedMap: $sneakpeak...")
            editor.putString(SAVED_MAP_JSON, cachedMap)

            editor.apply()
        }
    }

    /**
     * Handles user clicks on the [BottomNavigationView], starting the corresponding activities.
     *
     * @param item the menu item clicked.
     * @return whether the click event was consumed.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(tag, "[onNavigationItemSelected] Clicked")
        when (item.itemId) {
            R.id.account_nav -> startFragment(accountFragment)
            R.id.messaging_nav -> startFragment(inboxFragment)
            else -> {
                // Must check if we have permission to track the user's location before
                // setting up the MapFragment
                checkLocationPermission()
            }
        }

        // Let it be known that we have handled the event appropriately
        return true
    }

    /**
     * Replaces the currently active fragment, if there is any to replace.
     *
     * @param fragment the Fragment to replace the currently active one with.
     */
    private fun startFragment(fragment: Fragment) {
        Log.d(tag, "[startFragment] Invoked")
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.fragmentContainer, fragment)
        fTransaction.addToBackStack(null)
        fTransaction.commit()
        Log.d(tag, "[startFragment] Committed transaction to fragment")
    }


    /**
     * Checks for permissions before setting up the [mapFragment].
     * If they have not been granted, instantiates [permissionsManager] and requests
     * the location permissions.
     */
    private fun checkLocationPermission() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "[enableLocation] Permissions granted")
            // We have the necessary permissions to track the user location and can safely
            // set up a MapFragment
            startFragment(mapFragment)
        } else {
            Log.d(tag, "[enableLocation] Permissions not granted")
            // The permissions have not been granted. Before doing anything else, request them
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    /**
     * Presents a dialog explaining to the user why the location permission is needed.
     */
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // Recall that the only permission we need to request is the user location.
        alert {
            title = "We need you!... to give us your location."
            message = ("Coinz is a location-driven game.\nIn order to function properly, "
                    + "we needs your permission to track your location. Otherwise we won't "
                    + "be able to tell when you're close enough to pick up a coin!")
            positiveButton("Got it!"){
                // Try again
                checkLocationPermission()
            }
        }.show()
    }

    /**
     * Listener for location permission results.
     * If granted, invokes sets up [mapFragment].
     *
     * @param granted whether the permission was granted.
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            startFragment(mapFragment)
        } else {
            Log.w(tag, "[onPermissionResult] Permissions not granted")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
