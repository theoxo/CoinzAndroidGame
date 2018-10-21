package com.coinzgame.theoxo.coinz

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.ArrayList

/**
 * The [IntentService] which handles the [GeofencingEvent]s for the [Geofence]s on the map.
 * Obtains information about the event and broadcasts it back to [MainActivity] so that
 * the event can be handled appropriately.
 */
class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {

    private val tag = "GeoTransIntentService"

    /**
     * Handler for the received [GeofencingEvent]s. Obtains information on the type of the
     * [Geofence] transition, and which geofences were triggered. Finally, broadcasts this
     * information back to [MainActivity].
     */
    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(tag, "[onHandleIntent] Error in intent: $geofencingEvent.errorCode")
            return
        }

        // Get transition type
        val geofenceTransition = geofencingEvent.geofenceTransition
        // Get the triggered geofences (may be multiple)
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        // Intent to return
        val lbmIntent = Intent(LBM_LISTENER)

        // Test that the reported transition was of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Transition is of interest; add the corresponding type info to the intent passed back
            lbmIntent.putExtra(LBM_TYPE_TAG, geofenceTransition)

            // Also add the IDs of the triggered geofences
            val ids : ArrayList<String>? = ArrayList()
            for (triggeringGeofence in triggeringGeofences) {
                ids?.add(triggeringGeofence.requestId)
            }
            lbmIntent.putStringArrayListExtra(LBM_ID_TAG, ids)
            Log.d(tag, "[onHandleIntent] Interesting transition found")
        } else {
            Log.d(tag, "Geofence event triggered but was not an interesting type")
        }

        // Broadcast the gathered details back to the MainActivity
        LocalBroadcastManager.getInstance(this).sendBroadcast(lbmIntent)
    }
}