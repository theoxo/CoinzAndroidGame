package com.coinzgame.theoxo.coinz

import android.app.Activity
import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

public const val LBM_LISTENER = "CoinEncountered"

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {

    private val TAG = "GeoTransIntentService"


    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "$geofencingEvent.errorCode")
            return
        }

        // Get transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the triggered geofences (may be multiple)
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            // TODO do more interesting things with the above
            Log.d(TAG, "[onHandleIntent] GEOFENCE_TRANSITION_ENTER found")
        } else {
            Log.d(TAG, "Geofence event triggered but was not an interesting type")
        }

        val lbmIntent : Intent = Intent(LBM_LISTENER)
        LocalBroadcastManager.getInstance(this).sendBroadcast(lbmIntent)
    }
}