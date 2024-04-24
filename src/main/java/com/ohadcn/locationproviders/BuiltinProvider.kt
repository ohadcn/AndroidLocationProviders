package com.ohadcn.locationproviders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

class BuiltinProvider : ILocationProvider, LocationListener {
    private val locationManager: LocationManager
    override val providerName: String = "Builtin"
    private val TAG = "BuiltinProvider"

    companion object {
        private var instance : BuiltinProvider? = null

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun getInstance(context: Context): BuiltinProvider {
            if (instance == null) {
                instance = BuiltinProvider(context)
            }
            return instance!!
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    constructor(context: Context) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.allProviders?.forEach {
            locationManager.requestLocationUpdates(it, 0, 0f, this)
        }
        if (!locationManager.allProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0f, this)
        }
        Log.d(TAG, "BuiltinProvider initialized")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Network: ${locationManager.getProviderProperties(LocationManager.NETWORK_PROVIDER)}")
        } else {
            Log.d(TAG, "network: ${locationManager.getProvider(LocationManager.NETWORK_PROVIDER)?.requiresCell()} ${locationManager.getProvider(LocationManager.NETWORK_PROVIDER)?.requiresNetwork()} ${locationManager.getProvider(LocationManager.NETWORK_PROVIDER)?.requiresSatellite()}")
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
    override fun onLocationChanged(p0: Location) {
        listeners.forEach { it.onLocationChanged(p0) }
        lastLocation = p0
    }

    override fun onProviderDisabled(provider: String) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
    }

    override fun onFlushComplete(requestCode: Int) {
    }
}