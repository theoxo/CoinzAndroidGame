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

class AncientCoinSpawner : BroadcastReceiver(), DownloadCompleteListener {

    private val tag = "AncientCoinSpawner"

    private var context: Context? = null
    private var currentDate: String? = null

    override fun downloadComplete(result: String) {
        val sneakpeak = result.take(25)
        Log.d(tag, "[downloadComplete] Result: $sneakpeak...")

        // Copy the saved context and currentDate here for thread-safe behaviour
        val thisContext = context
        val thisDate = currentDate

        if (result == NETWORK_ERROR) {
            val title = "Coinz Network Error"
            val text = ("Ancient Coins will not be able to spawn before today's map is "
                        + "successfully downloaded.")
            if (thisContext != null) {
                // Let the user know that the download failed.
                displayNotificationWithTitleAndText(title, text)
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
                displayNotificationWithTitleAndText(title, text)

            } else {
                Log.w(tag, "[downloadComplete] context or date is null. Map will have to be "
                          + "downloaded again for the next alarm.")
            }

            // Finally, attempt to spawn ancient coins based on the download result
            spawnAncientCoins(result)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "onReceive fired")
        // Save the context received so it can be re-used for notifications
        this.context = context
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == FIRST_RUN_ACTION) {
            // Phone either just finished booting up or we are running the app for the first time.
            // Set up the alarms to listen for the desired times.
            if (context == null) {
                Log.e(tag, "[onReceive] context is null")
            } else {

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

                val desiredHours = listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)
                for (hour in desiredHours) {
                    // Set up two alarms; one for the hour precisely and one for half an hour later.

                    val currentSystemTime = System.currentTimeMillis()

                    val alarmCalendar : Calendar = Calendar.getInstance().apply {
                        timeInMillis = currentSystemTime
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                    }

                    val alarmIntent = Intent(context, AncientCoinSpawner::class.java)
                    alarmIntent.action = ALARM_ACTION
                    var alarmPendingIntent =
                            PendingIntent.getBroadcast(context,
                                    currentSystemTime.toInt()+hour,
                                    alarmIntent,
                                    PendingIntent.FLAG_ONE_SHOT)

                    if (alarmCalendar.timeInMillis < currentSystemTime) {
                        // If the time has already passed today, set the alarm to start tomorrow
                        // instead
                        alarmCalendar.add(Calendar.DATE, 1)
                    }

                    alarmManager?.setRepeating(
                            AlarmManager.RTC,
                            alarmCalendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            alarmPendingIntent
                    )

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


                    // Add one to the request code of the alarm pending intent to make sure it
                    // is unique
                    alarmPendingIntent =
                            PendingIntent.getBroadcast(context,
                                    currentSystemTime.toInt()+hour+1,
                                    alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                    alarmManager?.setRepeating(
                            AlarmManager.RTC,
                            alarmCalendarHalf.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            alarmPendingIntent
                    )
                }

                Log.d(tag, "Set up alarms")
            }
        } else if (intent?.action == ALARM_ACTION) {
            // Have received one of our alarms. Attempt to spawn ancient coins
            Log.d(tag, "Received alarm")
            if (context != null) {
                // Get current date
                val year : String = Calendar.getInstance().get(Calendar.YEAR).toString()
                var month : String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()  // Add one as 0-indexed
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
                val storedPrefs = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
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
                    // The user has already downloaded today's map.
                    spawnAncientCoins(cachedMap)
                }


            }
        }
    }

    private fun spawnAncientCoins(cachedMap: String) {

        val preferenceContext = context
        if (preferenceContext == null) {
            Log.e(tag, "[spawnAncientCoins] the context is null so cannot obtain stored "
                    + "preferences. Returning")
            return
        }
        val storedPrefs = preferenceContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        val topCoinValues = getTopCoinValues(cachedMap)

        val ancientCoinsSpawned = ArrayList<JSONObject>()
        val shilAncientCoin = generateAncientCoin(topCoinValues[0], "SHIL")
        val quidAncientCoin = generateAncientCoin(topCoinValues[1], "QUID")
        val dolrAncientCoin = generateAncientCoin(topCoinValues[2], "DOLR")
        val penyAncientCoin = generateAncientCoin(topCoinValues[3], "PENY")

        val editor: SharedPreferences.Editor = storedPrefs.edit()
        // TODO make the below neater
        if (shilAncientCoin != null) {
            Log.d(tag, "Saving an ancient shil coin")
            ancientCoinsSpawned.add(shilAncientCoin)
            editor.putString("Ancient SHIL coin", shilAncientCoin.toString())
        } else {
            Log.d(tag, "Setting saved ancient shil coin to empty string")
            editor.putString("Ancient SHIL coin", "")
        }
        if (quidAncientCoin != null) {
            ancientCoinsSpawned.add(quidAncientCoin)
            editor.putString("Ancient QUID coin", quidAncientCoin.toString())
            Log.d(tag, "Saving an ancient quid coin")
        } else {
            Log.d(tag, "Setting saved ancient quid coin to empty string")
            editor.putString("Ancient QUID coin", "")
        }
        if (dolrAncientCoin != null) {
            ancientCoinsSpawned.add(dolrAncientCoin)
            editor.putString("Ancient DOLR coin", dolrAncientCoin.toString())
            Log.d(tag, "Saving an ancient dolr coin")
        } else {
            Log.d(tag, "Setting saved ancient dolr coin to empty string")
            editor.putString("Ancient DOLR coin", "")
        }
        if (penyAncientCoin != null) {
            ancientCoinsSpawned.add(penyAncientCoin)
            editor.putString("Ancient PENY coin", penyAncientCoin.toString())
            Log.d(tag, "Saving an ancient peny coin")
        } else {
            Log.d(tag, "Setting saved ancient peny coin to empty string")
            editor.putString("Ancient PENY coin", "")
        }

        editor.apply()

        if (ancientCoinsSpawned.isNotEmpty()) {
            displayNotificationForAncientCoinsSpawned(ancientCoinsSpawned)
        }
    }

    private fun getTopCoinValues(geoJsonString: String) : Array<Double> {
        var topSHIL = 0.0
        var topQUID = 0.0
        var topDOLR = 0.0
        var topPENY = 0.0

        val features = FeatureCollection.fromJson(geoJsonString).features()
        if (features == null) {
            Log.e(tag, "[getTopCoinValues] features are null")
        } else {
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

    private fun generateAncientCoin(topCoinValue: Double, currency: String) : JSONObject? {

        // The json to return. Note this will remain null unless the coin flip is successful
        var json : JSONObject? = null

        val p = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        if (p < 1/32) {  // TODO change this to 1/32
            // Success! The coin shall be spawned.
            // Get it's value as 5 * the given top value of its currency:
            val value = (topCoinValue * 5).toString()
            // Randomly generate its location on the UoE campus
            val lat = ThreadLocalRandom.current().nextDouble(55.942617, 55.946233)
            val long = ThreadLocalRandom.current().nextDouble(-3.192473, -3.18419)

            // Generate a pseudo-unique id for the ancient coin
            val id = "ANCIENT$currency${System.currentTimeMillis().toInt()}"

            json = JSONObject()
            json.put("type", "Feature")
            val propertiesJson = JSONObject()
            propertiesJson.put("id", id)
            propertiesJson.put("value", value)
            propertiesJson.put("currency", currency)
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

    private fun displayNotificationForAncientCoinsSpawned(coins: ArrayList<JSONObject>) {

        Log.d(tag, "[displayNotificationForAncientCoinsSpawned] Invoked")

        if (coins.isEmpty()) {
            // This method must've been called erroneously. Finish early
            // before displaying any notification
            return
        }

        val notificationTitle : String = when (coins.size) {
            1 -> "An Ancient Coin Just Spawned!"
            else -> "${coins.size} Ancient Coins Just Spawned!"
        }

        var notificationBody = ""
        for (coinJson in coins) {
            // Add a short description for reach ancient coin
            val currency = coinJson.getJSONObject("properties").getString("currency")
            val value = coinJson.getJSONObject("properties").getString("value")
            notificationBody += "* $value $currency"
        }

        displayNotificationWithTitleAndText(notificationTitle, notificationBody)

    }

    private fun displayNotificationWithTitleAndText(title : String,
                                                    text : String) {

        val notificationContext = context

        if (notificationContext == null) {
            // Won't be able to set up the notification. Return early
            return
        }

        // If the context is non-null, go ahead and create the notification.

        // Set up the notification channel if on API >= 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(COINZ_CHANNEL_ID, COINZ_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = notificationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(notificationContext, COINZ_CHANNEL_ID)
                .setSmallIcon(R.mipmap.coinz_logo)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // display the notification
        with(NotificationManagerCompat.from(notificationContext)) {
            notify(0, notificationBuilder.build())
        }
    }
}