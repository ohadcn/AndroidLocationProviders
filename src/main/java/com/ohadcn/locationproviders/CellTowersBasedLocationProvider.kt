package com.ohadcn.locationproviders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CellTowersBasedLocationProvider : ILocationProvider {
    var considerIp: Boolean = true
    override val providerName: String = "CellTowers"

    private val telephonyManager: TelephonyManager

    private val time : Long

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun runUpdater(handler: Handler) {
        while (listeners.isNotEmpty()) {
            val startTime = System.currentTimeMillis()
            val location = getMyLocation()
            handler.post{
                listeners.forEach {
                    it.onLocationChanged(location)
                }
            }
            delay(time - (System.currentTimeMillis() - startTime))
        }
    }

    companion object {
        private var instance : CellTowersBasedLocationProvider? = null

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun getInstance(context: Context): CellTowersBasedLocationProvider {
            if (instance == null) {
                instance = CellTowersBasedLocationProvider(context)
            }
            return instance!!
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private constructor(context: Context, freq: Int = 1) {
        time = (60 * 1000 / freq).toLong()
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.requestCellInfoUpdate(context.mainExecutor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(p0: MutableList<CellInfo>) {
                        Log.d("CellTowersBasedLocationProvider", "onCellInfo: $p0")
                    }
                })
        } else {
            telephonyManager.listen(object : PhoneStateListener() {
                override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                    Log.d("CellTowersBasedLocationProvider", "onCellInfoChanged: $cellInfo")
                }
            }, PhoneStateListener.LISTEN_CELL_INFO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager.requestCellInfoUpdate(context.mainExecutor, object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                    Log.d("CellTowersBasedLocationProvider", "onCellInfo: $cellInfo")
                }
            })
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun startLocationUpdates(listener: LocationListener) {
        super.startLocationUpdates(listener)
        if (listeners.size == 1) {
            GlobalScope.launch { runUpdater(Handler(Looper.getMainLooper())) }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public fun getCellInfo(): List<CellInfo> {
        return telephonyManager.allCellInfo
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getMyLocation(): Location {
        val cells = telephonyManager.allCellInfo
        var location = Location(providerName).apply {
            latitude = 0.0
            longitude = 0.0
        }

        val withLocation = cells.filter {
            it is CellInfoCdma
        }
        if (withLocation.isNotEmpty()) {
            var lat = 0.0
            var lon = 0.0
            var ammount = 0
            withLocation.forEach {
                val cell = it as CellInfoCdma
                val weight = it.cellSignalStrength.level
                lat += cell.cellIdentity.latitude.toDouble() * weight
                lon += cell.cellIdentity.longitude.toDouble() * weight
                ammount += weight
            }
            lat /= ammount
            lon /= ammount
            location.latitude = lat
            location.longitude = lon
        } else {
            location = GoogleGeoLocationHelper.getGeoLocationFromCellTowers(cells, considerIp) ?: location
        }

//        (cell as CellInfoLte).also {
//            Log.d("CellTowersBasedLocationProvider", "getMyLocation: $it")
//            Log.d("CellTowersBasedLocationProvider", "getMyLocation: ${it.cellIdentity}")
////            Log.d("CellTowersBasedLocationProvider", "getMyLocation: ${it.cellIdentity}")
//            Log.d("CellTowersBasedLocationProvider", "getMyLocation: ${it.cellSignalStrength}")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                Log.d("CellTowersBasedLocationProvider", "getMyLocation: ${it.cellSignalStrength.level} ${it.cellSignalStrength.dbm} ${it.cellSignalStrength.asuLevel} ${it.cellSignalStrength.timingAdvance} ${it.cellSignalStrength.cqi} ${it.cellSignalStrength.rsrp} ${it.cellSignalStrength.rsrq} ${it.cellSignalStrength.rssnr}")
//            }
//
//
//        }



        return location
    }
}