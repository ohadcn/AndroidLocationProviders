package com.ohadcn.locationproviders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

class GnssProvider : ILocationProvider {
    private val locationManager: LocationManager
    override val providerName: String = "Gnss"
    private val TAG = "GnssProvider"

    companion object {
        private var instance : GnssProvider? = null

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun getInstance(context: Context): GnssProvider {
            if (instance == null) {
                instance = GnssProvider(context)
            }
            return instance!!
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    constructor(context: Context) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.registerGnssMeasurementsCallback(object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                Log.d(TAG, "onGnssMeasurementsReceived: $event")
                Log.d(TAG, "onGnssMeasurementsReceived: ${event.clock}")
                event.measurements.forEach() {
                    println("Measurement: $it")
                    println("Measurement: $it.")
                }
            }
         }, null)

        locationManager.registerGnssNavigationMessageCallback(object : GnssNavigationMessage.Callback() {
            override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
                when (event.type) {
                    GnssNavigationMessage.TYPE_GPS_L1CA -> {
//                        Log.d(TAG, "GPS_L1CA onGnssNavigationMessageReceived: ${event.svid} ${event.type} ${event.status} ${event.messageId} ${event.submessageId} ${event.data.joinToString(",")}")

                    }
                    GnssNavigationMessage.TYPE_GLO_L1CA -> {

                    }
                    GnssNavigationMessage.TYPE_BDS_CNAV1 -> {

                    }
                    GnssNavigationMessage.TYPE_BDS_CNAV2 -> {

                    }
                    GnssNavigationMessage.TYPE_BDS_D1 -> {
                        Log.d(TAG, "BDS_D1 onGnssNavigationMessageReceived: ${event.svid} ${event.type} ${event.status} ${event.messageId} ${event.submessageId} ${event.data.joinToString(",")}")
                    }
                    else -> {
                        Log.d(TAG, "onGnssNavigationMessageReceived: ${event.type}")
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(status: Int) {
                Log.d(TAG, "onStatusChanged: $status")
            }
        }, null)
        locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
            override fun onStarted() {
                Log.d(TAG, "onStarted")
            }
            override fun onStopped() {
                Log.d(TAG, "onStopped")
            }
            override fun onFirstFix(ttffMillis: Int) {
                Log.d(TAG, "onFirstFix: $ttffMillis")
            }
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                Log.d(TAG, "onSatelliteStatusChanged: $status ${status.satelliteCount}")
            }
        }, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "gnssCapabilities ${locationManager.gnssCapabilities}")
            Log.d(TAG, "gnssYearOfHardware ${locationManager.gnssYearOfHardware}")
        }
    }


    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun startLocationUpdates(listener: LocationListener) {
        super.startLocationUpdates(listener)
    }

    override fun getMyLocation(): Location {
        return lastLocation ?: Location("BuiltinProvider").apply {
            latitude = 0.0
            longitude = 0.0
        }
    }

    var lastLocation: Location? = null
}