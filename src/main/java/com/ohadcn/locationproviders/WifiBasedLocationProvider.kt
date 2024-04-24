package com.ohadcn.locationproviders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL

class WifiBasedLocationProvider : ILocationProvider {
    private var time: Long
    private val wifiManager: WifiManager
    override val providerName: String = "Wifi"
    private val TAG = "WifiProvider"
    var considerIp: Boolean = true
    var lastLocation: Location = Location("Wifi").apply {
        latitude = 0.0
        longitude = 0.0
    }

    companion object {
        private var instance: WifiBasedLocationProvider? = null

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun getInstance(context: Context): WifiBasedLocationProvider {
            if (instance == null) {
                instance = WifiBasedLocationProvider(context)
            }
            return instance!!
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun runUpdater(handler: Handler) {
        while (listeners.isNotEmpty()) {
            val startTime = System.currentTimeMillis()
            val location = getMyLocationOld()
            listeners.forEach {
                handler.post {
                    it.onLocationChanged(location)
                }
            }
            delay(time - (System.currentTimeMillis() - startTime))
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    constructor(context: Context, freq: Int = 1) {
        time = (60000 / freq).toLong()
        wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            wifiManager.registerScanResultsCallback(context.mainExecutor, object : WifiManager.ScanResultsCallback() {
//                override fun onScanResultsAvailable() {
//                    val res = wifiManager.scanResults
//                    Log.d(TAG, "onScanResultsAvailable: $res")
//                    lastLocation = fromScan(res)
//                    listeners.forEach { it.onLocationChanged(lastLocation) }
//                }
//            })
//            lastLocation = fromScan(wifiManager.scanResults)
//            listeners.forEach { it.onLocationChanged(lastLocation) }
//        } else {
        Log.d(TAG, "old phone, using old method")
        GlobalScope.launch { runUpdater(Handler(Looper.getMainLooper())) }
//        }
    }


    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun startLocationUpdates(listener: LocationListener) {
        super.startLocationUpdates(listener)
        listener.onLocationChanged(lastLocation)
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getMyLocationOld(): Location {
        wifiManager.startScan()
        val res = wifiManager.scanResults
        return fromScan(res)
    }

    @SuppressLint("MissingPermission")
    public fun getScanResults(): List<ScanResult> {
        return wifiManager.scanResults
    }

    private fun fromScan(res: List<ScanResult>): Location {
        lastLocation =
            GoogleGeoLocationHelper.getGeoLocationFromWifi(res, considerIp) ?: lastLocation
        return lastLocation
    }

    override fun getMyLocation(): Location {
        return lastLocation
    }

}