package com.coinzgame.theoxo.coinz

import android.app.Activity
import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.ArrayList

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
        // Get the triggered geofences (may be multiple)
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        val lbmIntent : Intent = Intent(LBM_LISTENER)

        // Test that the reported transition was of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            lbmIntent.putExtra("type", Geofence.GEOFENCE_TRANSITION_ENTER)
            val ids : ArrayList<String>? = ArrayList()
            for (triggeringGeofence in triggeringGeofences) {
                ids?.add(triggeringGeofence.requestId)
                lbmIntent.putExtra("id", triggeringGeofence.requestId)
            }
            lbmIntent.putStringArrayListExtra("ids", ids)
            Log.d(TAG, "[onHandleIntent] GEOFENCE_TRANSITION_ENTER found")
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            lbmIntent.putExtra("type", Geofence.GEOFENCE_TRANSITION_EXIT)
            val ids : ArrayList<String>? = ArrayList()
            for (triggeringGeofence in triggeringGeofences) {
                ids?.add(triggeringGeofence.requestId)
                lbmIntent.putExtra("id", triggeringGeofence.requestId)
            }
            lbmIntent.putStringArrayListExtra("ids", ids)
            Log.d(TAG, "[onHandleIntent] GEOFENCE_TRANSITION_EXIT found")
        } else {
            Log.d(TAG, "Geofence event triggered but was not an interesting type")
        }


        LocalBroadcastManager.getInstance(this).sendBroadcast(lbmIntent)
    }
}