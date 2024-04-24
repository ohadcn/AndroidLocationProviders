package com.ohadcn.locationproviders

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import androidx.annotation.RequiresPermission

abstract class ILocationProvider {

    abstract val providerName: String

    protected val listeners = mutableListOf<LocationListener>()

    companion object {

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        public fun startAllLocationUpdates(listener: LocationListener, context: Context, considerIp: Boolean) {
            GoogleGeoLocationHelper.setApiKey(context.applicationInfo.metaData.getString("com.google.android.geo.API_KEY")!!)
            BuiltinProvider.getInstance(context).startLocationUpdates(listener)
            CellTowersBasedLocationProvider.getInstance(context).startLocationUpdates(listener)
            GnssProvider.getInstance(context).startLocationUpdates(listener)
            WifiBasedLocationProvider.getInstance(context).startLocationUpdates(listener)
            considerIp(context, considerIp)
        }

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun considerIp(context: Context, considerIp: Boolean) {
            WifiBasedLocationProvider.getInstance(context).considerIp = considerIp
            CellTowersBasedLocationProvider.getInstance(context).considerIp = considerIp
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    open fun startLocationUpdates(listener: LocationListener) {
        listeners.add(listener)
    }
    fun stopLocationUpdates(listener: LocationListener) {
        listeners.remove(listener)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    abstract fun getMyLocation(): Location
}