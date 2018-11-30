package com.coinzgame.theoxo.coinz

import android.content.*
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.coinzgame.theoxo.coinz.R.id.home_nav
import com.google.firebase.firestore.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import java.util.*

/**
 * The app's main Activity.
 * Handles various aspects of loading and displaying the map,
 * tracking the user's location, marking the coins on the map and picking them up.
 */
class MainActivity : AppCompatActivity(), PermissionsListener,
        BottomNavigationView.OnNavigationItemSelectedListener {//, LocationEngineListener,
        //OnMapReadyCallback, DownloadCompleteListener,
        //BottomNavigationView.OnNavigationItemSelectedListener {

    private val tag = "MainActivity"

    // Local variables related to the location tracking and displaying
    private lateinit var permissionsManager : PermissionsManager

    // Locally saved data tracking
    internal var currentDate : String? = null // FORMAT YYYY/MM/DD
    internal var lastDownloadDate : String? = null
    internal var cachedMap : String? = null
    internal var ancientCoins = ArrayList<Feature>()

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    internal var firestoreWallet : DocumentReference? = null
    internal var firestoreInbox: DocumentReference? = null
    internal var currentUserEmail : String? = null

    private var mapFragment: MapFragment = MapFragment()
    private var inboxFragment: InboxFragment = InboxFragment()
    private var accountFragment: AccountFragment = AccountFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, MAPBOX_KEY)
        setContentView(R.layout.activity_main)
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
                        + "click on them.\n\nYou can then deposit the coins you've collected "
                        + "into the bank in exchange for gold, or send them to your friends!")
                title = "Hello there... Looks like you're new!"
                positiveButton("Got it!"){}
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

        val emailTag = currentUserEmail
        if (emailTag != null) {
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)
            firestoreInbox = firestore?.collection(emailTag)?.document(INBOX_DOCUMENT)
        } else {
            Log.d(tag, "[onCreate] emailTag is null")
        }
    }

    /**
     * Fetches the preferences stored on the device if necessary.
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
        val year : String = Calendar.getInstance().get(Calendar.YEAR).toString()
        // Add one to the month as it is 0-indexed
        var month : String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        var day : String = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
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

        // Also get the current ancient coins
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

        checkLocationPermission()
    }

    /**
     * Saves the user preferences if needed.
     * The user preferences are stored on the device only if [lastDownloadDate]
     * has changed since the last time they were stored.
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
     * @param item the menu item clicked
     */
    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        Log.d(tag, "[onNavigationitemSelected] Clicked")
        when (item.itemId) {
            R.id.account_nav -> startAccountFragment()
            R.id.messaging_nav -> startInboxFragment()
            else -> checkLocationPermission()
        }
        return true
    }

    /**
     * Starts a new [AccountFragment].
     */
    private fun startAccountFragment() {
        Log.d(tag, "[startAccountFragment] Invoked")
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.fragmentContainer, accountFragment)
        fTransaction.addToBackStack(null)
        fTransaction.commit()
        Log.d(tag, "Committed transaction to AccountFragment")
    }

    /**
     * Starts a new [InboxFragment].
     */
    private fun startInboxFragment() {
        Log.d(tag, "[startInboxFragment] Invoked")
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.fragmentContainer, inboxFragment)
        fTransaction.addToBackStack(null)
        fTransaction.commit()
        Log.d(tag, "Committed transaction to InboxFragment")
    }



    /**
     * Checks for permissions before making further calls to set up the location tracking.
     * If the necessary location permissions have been granted, invokes the
     * async call to get the [mapboxMap] ([MapboxMap]) instance.
     * If not, instantiates a [PermissionsManager] and requests the location permissions.
     */
    private fun checkLocationPermission() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "[enableLocation] Permissions granted")
            Log.d(tag, "Setting up ftransaction to MapFragment")
            val fManager = supportFragmentManager
            val fTransaction = fManager.beginTransaction()
            fTransaction.replace(R.id.fragmentContainer, mapFragment)
            fTransaction.addToBackStack(null)
            fTransaction.commit()
            Log.d(tag, "Committed transaction to MapFragment")
        } else {
            Log.d(tag, "[enableLocation] Permissions not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // TODO present dialog stating why permissions are needed
    }

    /**
     * Listener for location permission results.
     * If granted, invokes [enableLocation].
     *
     * @param granted whether the permission was granted.
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Log.d(tag, "Setting up ftransaction to MapFragment")
            val fManager = supportFragmentManager
            val fTransaction = fManager.beginTransaction()
            fTransaction.replace(R.id.fragmentContainer, mapFragment)
            fTransaction.addToBackStack(null)
            fTransaction.commit()
            Log.d(tag, "Committed transaction to MapFragment")
        } else {
            Log.e(tag, "[onPermissionResult] Permissions not granted")
            // TODO explain to user why necessary
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
