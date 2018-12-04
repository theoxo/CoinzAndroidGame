package com.coinzgame.theoxo.coinz

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.gson.JsonObject
import com.mapbox.geojson.FeatureCollection
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Provides means to set up and listen for alarms triggering ancient coins spawning.
 * Also provides functionality to download the day's map when such an alarm is triggered,
 * if it has not already been downloaded.
 */
class AncientCoinSpawner : BroadcastReceiver(), DownloadCompleteListener {

    private val tag = "AncientCoinSpawner"

    private var context: Context? = null
    private var currentDate: String? = null

    /**
     * Listens for the background map download to finish.
     *
     * @param result the GeoJSON String which was built from the downloaded data.
     */
    override fun downloadComplete(result: String) {
        val sneakpeek = result.take(25)
        Log.d(tag, "[downloadComplete] Result: $sneakpeek...")

        // Copy the saved context and currentDate here for thread-safe behaviour
        val thisContext = context
        val thisDate = currentDate

        if (result == NETWORK_ERROR) {
            // The background download encountered an exception,
            // construct an appropriate notification and let the user know
            val title = "Coinz Network Error"
            val text = ("Ancient Coins will not be able to spawn before today's map is "
                        + "successfully downloaded.")
            if (thisContext != null) {
                // Let the user know that the download failed.
                displayNotificationWithTitleAndText(title, text, COINZ_DOWNLOAD_NOTIFICATION_ID)
            }
        } else {
            // We have successfully downloaded today's map. Update this in the local storage.
            if (thisContext != null && thisDate != null) {

                val storedPrefs = thisContext.getSharedPreferences(PREFERENCES_FILE,
                        Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = storedPrefs.edit()
                editor.putString(LAST_DOWNLOAD_DATE, thisDate)
                editor.putString(SAVED_MAP_JSON, result)
                editor.apply()

                // We should also notify the user that the download was completed in the background.
                val title = "Coinz Background Download"
                val text = "Today's map has been downloaded onto your device."
                displayNotificationWithTitleAndText(title, text, COINZ_DOWNLOAD_NOTIFICATION_ID)

            } else {
                Log.w(tag, "[downloadComplete] context or date is null. Map will have to be "
                          + "downloaded again for the next alarm.")
            }

            // Finally, attempt to spawn ancient coins based on the download result
            spawnAncientCoins(result)
        }
    }

    /**
     * Handles the received intent, either setting up the alarms or handling one of them triggering.
     *
     * @param context the context, or null.
     * @param intent the intent received, or null.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "onReceive fired")
        // Save the context received so it can be re-used for notifications
        this.context = context

        // Process the intent according to its action
        when {
            context == null -> {
                Log.e(tag, "[onReceive] context is null")
            }

            (intent?.action == Intent.ACTION_BOOT_COMPLETED
                    || intent?.action == FIRST_RUN_ACTION) -> {
                // Phone either just finished booting up or we are running the app for the first
                // time.
                // Set up the alarms to listen for the desired times.
                setUpAlarms(context)
            }

            intent?.action == ALARM_ACTION -> {
                // Have received one of our alarms. Attempt to spawn ancient coins
                handleSpawnAlarm(context)
            }

            intent?.action == OVERWRITE_ALARM_ACTION -> {
                // The hour has struck 19.00 and we therefore want to make sure
                // that no ancient coins are saved on the device
                saveAncientCoins(null, null, null, null)

            }
            else -> {
                // These are the only types of intents we expect.
                Log.w(tag, "[onReceive] Could not recognize intent action ${intent?.action}")
            }

        }
    }

    /**
     * Handles one of our ancient coin spawn alarms triggering.
     * First makes sure we have today's map downloaded and then invokes [spawnAncientCoins].
     *
     * @param context the context in which to get the data stored on the device.
     */
    private fun handleSpawnAlarm(context: Context) {
        // Get current date
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

        // Get the data stored on the system to see if the user has already downloaded
        // today's map
        val storedPrefs = context.getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE)
        val cachedMap = storedPrefs.getString(SAVED_MAP_JSON, null)
        val lastDownloadDate = storedPrefs.getString(LAST_DOWNLOAD_DATE, "")

        if (lastDownloadDate != currentDate || cachedMap == null) {
            // The user has not yet downloaded the map for today.
            // Do this in the background so we can spawn today's ancient coins.
            Log.e(tag, "[onReceive] cachedMap is invalid. Downloading a new one")
            // Begin the background download
            val dateString = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
            DownloadFileTask(this).execute(dateString)
        } else {
            // The user has already downloaded today's map, go ahead and try to spawn the
            // ancient coins for this alarm.
            spawnAncientCoins(cachedMap)
        }
    }

    /**
     * Saves any and all ancient coins generated to the user's device.
     *
     * @param shil the ancient coin of the SHIL currency, or null
     * @param quid the ancient coin of the QUID currency, or null
     * @param dolr the ancient coin of the DOLR currency, or null
     * @param peny the ancient coin of the PENY currency, or null
     */
    private fun saveAncientCoins(shil: JSONObject?, quid: JSONObject?, dolr: JSONObject?,
                                 peny: JSONObject?) {
        val preferenceContext = context
        if (preferenceContext == null) {
            // If the context is null we won't be able to store the ancient coins we've spawned.
            // Throw an error log and return early
            Log.e(tag, "[saveAncientCoins] the context is null so cannot obtain stored "
                    + "preferences. Returning")
            return
        }

        // Set up the references to the shared preferences file
        val storedPrefs = preferenceContext.getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = storedPrefs.edit()

        if (shil != null) {
            Log.d(tag, "Saving an ancient shil coin")
            editor.putString(ANCIENT_SHIL, shil.toString())
        } else {
            Log.d(tag, "Setting saved ancient shil coin to empty string")
            editor.putString(ANCIENT_SHIL, "")
        }

        if (quid != null) {
            editor.putString(ANCIENT_QUID, quid.toString())
            Log.d(tag, "Saving an ancient quid coin")
        } else {
            Log.d(tag, "Setting saved ancient quid coin to empty string")
            editor.putString(ANCIENT_QUID, "")
        }

        if (dolr != null) {
            editor.putString(ANCIENT_DOLR, dolr.toString())
            Log.d(tag, "Saving an ancient dolr coin")
        } else {
            Log.d(tag, "Setting saved ancient dolr coin to empty string")
            editor.putString(ANCIENT_DOLR, "")
        }

        if (peny != null) {
            editor.putString(ANCIENT_PENY, peny.toString())
            Log.d(tag, "Saving an ancient peny coin")
        } else {
            Log.d(tag, "Setting saved ancient peny coin to empty string")
            editor.putString(ANCIENT_PENY, "")
        }

        // Save the changes to the preferences file.
        editor.apply()
    }

    /**
     * Set up the alarms for the ancient coin spawn timers on the device.
     *
     * @param context the context to set up the Intents in.
     */
    private fun setUpAlarms(context: Context) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        /*
        val alarmIntent1 = Intent(context, AncientCoinSpawner::class.java)
        alarmIntent1.action = ALARM_ACTION

        // Get the current system time to build the alarms based off
        val currentSystemTime1 = System.currentTimeMillis()

        // Set up the calendar for the first intent
        val alarmCalendar1 : Calendar = Calendar.getInstance().apply {
            timeInMillis = currentSystemTime1
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 15)
        }

        val alarmPendingIntent1 =
                PendingIntent.getBroadcast(context,
                        currentSystemTime1.toInt(),
                        alarmIntent1,
                        PendingIntent.FLAG_UPDATE_CURRENT)



        // Set up the first alarm.
        alarmManager?.setRepeating(
                AlarmManager.RTC,
                alarmCalendar1.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                alarmPendingIntent1
        )*/
        val desiredHours = listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)
        for (hour in desiredHours) {
            // Set up two alarms; one for the hour precisely and one for half an hour later.

            // Set ourselves to be the receiver of the intents for the alarms
            // and set the action so we can identify it when we receive it.
            val alarmIntent = Intent(context, AncientCoinSpawner::class.java)
            alarmIntent.action = ALARM_ACTION

            // Get the current system time to build the alarms based off
            val currentSystemTime = System.currentTimeMillis()

            // Set up the calendar for the first intent
            val alarmCalendar : Calendar = Calendar.getInstance().apply {
                timeInMillis = currentSystemTime
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
            }

            if (alarmCalendar.timeInMillis < currentSystemTime) {
                // If the time has already passed today, set the alarm to start tomorrow
                // instead
                alarmCalendar.add(Calendar.DATE, 1)
            }

            val alarmPendingIntent =
                    PendingIntent.getBroadcast(context,
                            currentSystemTime.toInt()+hour,
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)



            // Set up the first alarm.
            alarmManager?.setRepeating(
                    AlarmManager.RTC,
                    alarmCalendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    alarmPendingIntent
            )

            // Set up the calendar for the second alarm at 30 minutes past the hour
            val alarmCalendarHalf : Calendar = Calendar.getInstance().apply {
                timeInMillis = currentSystemTime
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 30)
            }

            if (alarmCalendarHalf.timeInMillis < currentSystemTime) {
                // If the time has already passed today, set the alarm to start tomorrow
                // instead
                alarmCalendarHalf.add(Calendar.DATE, 1)
            }

            val alarmIntentHalf = Intent(context, AncientCoinSpawner::class.java)
            alarmIntentHalf.action = ALARM_ACTION

            // Add one to the request code of the alarm pending intent to make sure it
            // is unique
            val alarmPendingIntentHalf =
                    PendingIntent.getBroadcast(context,
                            currentSystemTime.toInt()+hour+1,
                            alarmIntentHalf, PendingIntent.FLAG_UPDATE_CURRENT)

            // Set up the second alarm.
            alarmManager?.setRepeating(
                    AlarmManager.RTC,
                    alarmCalendarHalf.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    alarmPendingIntentHalf
            )
        }

        // Also set up an alarm for 19.00 that overwrites any coins spawned at 18.30 with
        // the empty string so they don't stay on the map all night
        // Set ourselves to be the receiver of the intents for the alarms
        // and set the action so we can identify it when we receive it.
        val overWriteIntent = Intent(context, AncientCoinSpawner::class.java)
        overWriteIntent.action = OVERWRITE_ALARM_ACTION

        val currentSystemTime = System.currentTimeMillis()

        // Set up the calendar for the overwrite event
        val overWriteCalendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = currentSystemTime
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
        }

        if (overWriteCalendar.timeInMillis < currentSystemTime) {
            // If the time has already passed today, set the alarm to start tomorrow
            // instead
            overWriteCalendar.add(Calendar.DATE, 1)
        }

        val overWritePendingIntent =
                PendingIntent.getBroadcast(context,
                        currentSystemTime.toInt(),
                        overWriteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT)



        // Set up the overwrite alarm
        alarmManager?.setRepeating(
                AlarmManager.RTC,
                overWriteCalendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                overWritePendingIntent
        )

        Log.d(tag, "[setUpAlarms] Alarm setup complete")

    }

    /**
     * Attempts to spawn the ancient coins for this alarm.
     * If successful will invoke [generateNotificationForAncientCoinsSpawned].
     *
     * @param geoJsonString the GeoJSON String with today's coins.
     */
    private fun spawnAncientCoins(geoJsonString: String) {

        // Keep track of which coins spawned so we can build the notification
        val ancientCoinsSpawned = ArrayList<JSONObject>()

        // Get the top coin values in the order SHIL, QUID, DOLR, PENY
        val topCoinValues = getTopCoinValues(geoJsonString)

        // Attempt to spawn an ancient SHIL
        val shilAncientCoin = generateAncientCoin(topCoinValues[0], "SHIL")
        if (shilAncientCoin != null) {
            ancientCoinsSpawned.add(shilAncientCoin)
        }

        // Now do the same for QUID
        val quidAncientCoin = generateAncientCoin(topCoinValues[1], "QUID")
        if (quidAncientCoin != null) {
            ancientCoinsSpawned.add(quidAncientCoin)
        }

        // Next up is the DOLR currency
        val dolrAncientCoin = generateAncientCoin(topCoinValues[2], "DOLR")
        if (dolrAncientCoin != null) {
            ancientCoinsSpawned.add(dolrAncientCoin)
        }

        // Finally, attempt to spawn an ancient PENY.
        val penyAncientCoin = generateAncientCoin(topCoinValues[3], "PENY")
        if (penyAncientCoin != null) {
            ancientCoinsSpawned.add(penyAncientCoin)
        }

        // Save any successes on the device
        saveAncientCoins(shilAncientCoin, quidAncientCoin, dolrAncientCoin, penyAncientCoin)

        // Build a notification if doing so is appropriate
        if (ancientCoinsSpawned.isNotEmpty()) {
            // We have successfully generated at least 1 ancient coin; let the user know!
            generateNotificationForAncientCoinsSpawned(ancientCoinsSpawned)
        }
    }

    /**
     * Gets an array of the highest value of each currency on today's map.
     *
     * @param geoJsonString the GeoJSON String with today's coins.
     * @return Each currency's top value, in the order SHIL, QUID, DOLR, PENY.
     */
    private fun getTopCoinValues(geoJsonString: String) : Array<Double> {
        var topSHIL = 0.0
        var topQUID = 0.0
        var topDOLR = 0.0
        var topPENY = 0.0

        val features = FeatureCollection.fromJson(geoJsonString).features()
        if (features == null) {
            Log.e(tag, "[getTopCoinValues] features are null")
        } else {
            // Feature-s are non-null. Loop over them safely, updating the
            // highest seen value for each currency as we go along
            for (feature in features) {
                // Extract information from the feature
                val properties: JsonObject? = feature.properties()
                val value: Double? = properties?.get("value")?.asDouble
                val currency: String? = properties?.get("currency")?.asString
                when {
                    value == null -> {
                        Log.e(tag, "[getTopCoinValues] value of coin is null")
                    }
                    currency == null -> {
                        Log.e(tag, "[getTopCoinValues] currency of coin is null")
                    }
                    currency == "SHIL" && value > topSHIL -> {
                        topSHIL = value
                    }
                    currency == "QUID" && value > topQUID -> {
                        topQUID = value
                    }
                    currency == "DOLR" && value > topDOLR -> {
                        topDOLR = value
                    }
                    currency == "PENY" && value > topPENY -> {
                        topPENY = value
                    }
                }
            }
        }


        Log.d(tag, "[getTopCoinValues] Found $topSHIL SHIL, $topQUID QUID, $topDOLR DOLR, "
            + "$topPENY PENY")

        return arrayOf(topSHIL, topQUID, topDOLR, topPENY)
    }

    /**
     * Attempts to generate an ancient coin given the currency and highest value thereof.
     *
     * @param topCoinValue the highest value of any coin on the map of the given currency.
     * @param currency the currency of the ancient coin to be generated.
     * @return the JSON specifying the ancient coin if it was spawned, or null otherwise.
     */
    private fun generateAncientCoin(topCoinValue: Double, currency: String) : JSONObject? {

        // The json to return. Note this will remain null unless the coin flip is successful
        var json : JSONObject? = null

        val p = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        if (p < 0.5) {
            // Success! The coin shall be spawned.
            // Make its value 5 * the given top value of its currency:
            val value = (topCoinValue * 5).toString()
            // Randomly generate its location on the UoE campus
            val lat = ThreadLocalRandom.current().nextDouble(UOE_MIN_LATITUDE, UOE_MAX_LATITUDE)
            val long = ThreadLocalRandom.current().nextDouble(UOE_MIN_LONGITUDE, UOE_MAX_LONGITUDE)

            // Generate a pseudo-unique id for the ancient coin
            val id = "ANCIENT$currency${System.currentTimeMillis().toInt()}"

            // Construct an appropriate Geo-JSON for the coin, following the same
            // style as the downloaded maps.
            json = JSONObject()
            json.put("type", "Feature")
            val propertiesJson = JSONObject()
            propertiesJson.put(ID, id)
            propertiesJson.put(VALUE, value)
            propertiesJson.put(CURRENCY, currency)
            json.put("properties", propertiesJson)
            val geometryJson = JSONObject()
            geometryJson.put("type", "Point")
            val coords = JSONArray()
            coords.put(long)
            coords.put(lat)
            geometryJson.put("coordinates", coords)
            json.put("geometry", geometryJson)
        }

        Log.d(tag, "[generateAncientCoin] returning ${json.toString()}")

        return json
    }

    /**
     * Builds an appropriate notification for the spawned ancient coins.
     * Then invokes [displayNotificationWithTitleAndText] to show the notification to the user.
     *
     * @param coins a list of the ancient coins which were just spawned.
     */
    private fun generateNotificationForAncientCoinsSpawned(coins: ArrayList<JSONObject>) {

        Log.d(tag, "[generateNotificationForAncientCoinsSpawned] Invoked")

        if (coins.isEmpty()) {
            // This method must've been called erroneously. Finish early
            // before displaying any notification
            return
        }

        // Create an appropriate title
        val notificationTitle : String = when (coins.size) {
            1 -> "An Ancient Coin Just Spawned!"
            else -> "${coins.size} Ancient Coins Just Spawned!"
        }

        // Create the notification's main text body
        var notificationBody = ""
        for (coinJson in coins) {
            // Add a short description for reach ancient coin
            val currency = coinJson.getJSONObject("properties").getString(CURRENCY)
            val value = coinJson.getJSONObject("properties").getString(VALUE)
            notificationBody += "* $value $currency"
        }

        // Attempt to fire off the notification
        displayNotificationWithTitleAndText(
                notificationTitle, notificationBody,COINZ_SPAWN_NOTIFICATION)

    }

    /**
     * Displays a notification with the specified parameters on the user's device.
     *
     * @param title the notification's title.
     * @param text the desired text for the notification.
     * @param id the id of the notification to display
     */
    private fun displayNotificationWithTitleAndText(title : String,
                                                    text : String,
                                                    id: Int) {

        Log.d(tag, "[displayNotificationWithTitleAndText] Invoked")
        val notificationContext = context

        if (notificationContext == null) {
            // Won't be able to set up the notification. Return early
            Log.e(tag, "[displayNotificationWithTitleAndText] Context is null, returning.")
            return
        }

        // If the context is non-null, go ahead and create the notification.

        // Set up the notification channel if on API >= 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(COINZ_CHANNEL_ID, COINZ_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = notificationContext.getSystemService(
                    Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }

        // Build an expendable notification which shows the full text upon expansion
        val notificationBuilder = NotificationCompat.Builder(notificationContext, COINZ_CHANNEL_ID)
                .setSmallIcon(R.mipmap.coinz_logo)
                .setContentTitle(title)
                .setContentText("${text.take(15)}...")
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Display the notification on the user's device.
        with(NotificationManagerCompat.from(notificationContext)) {
            notify(id, notificationBuilder.build())
        }
    }
}