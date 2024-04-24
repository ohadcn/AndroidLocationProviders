package com.ohadcn.locationproviders

import android.content.Context
import android.location.Location
import android.net.wifi.ScanResult
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.net.HttpURLConnection
import java.net.URL

class GoogleGeoLocationHelper {
    companion object {
        private val TAG: String = GoogleGeoLocationHelper::class.java.simpleName
        private var url =
            URL("https://www.oodi.co.il/sattelites.php")

        public fun setApiKey(apiKey: String) {
            url = URL("https://www.googleapis.com/geolocation/v1/geolocate?key=${apiKey}")
        }

        fun getGeoLocationFromWifi(res: List<ScanResult>, considerIp: Boolean): Location? {
            var location: Location? = null
            try {
                var conn = url.openConnection()
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.getOutputStream()
                    .write("{\"considerIp\": ${considerIp},\"wifiAccessPoints\": [".toByteArray())
                res.forEachIndexed { i, it ->
                    conn.getOutputStream()
                        .write("{\"macAddress\": \"${it.BSSID}\", \"signalStrength\": ${it.level},\"channel\":${when(it.frequency){
                            in 2412..2484 -> (it.frequency - 2412) / 5 + 1
                            in 5170..5825 -> (it.frequency - 5170) / 5 + 34
                            else -> 0
                        }}}".toByteArray())
                    if (i < res.size - 1) {
                        conn.getOutputStream().write(",".toByteArray())

                    }
                }
                conn.getOutputStream().write("]}".toByteArray())
                val loc = conn.getInputStream().bufferedReader().readText()
                val lat = loc.substringAfter("lat\":").substringBefore(",")
                val lon = loc.substringAfter("lng\":").substringBefore("}")
                val acc =
                    loc.substringAfter("accuracy\":").substringBefore(",").substringBefore("}")
                location = Location("WIFI").apply {
                    latitude = lat.toDouble()
                    longitude = lon.toDouble()
                    accuracy = acc.toFloat() / 100.0f
                }
            } catch (e: Exception) {
                Log.e(TAG, "getMyLocation: ", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            return location
        }

        fun getGeoLocationFromCellTowers(res: List<CellInfo>, considerIp : Boolean): Location? {
            var location: Location? = null
            try {
                var conn = url.openConnection()
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.getOutputStream()
                    .write("{\"considerIp\": ${considerIp},\"cellTowers\": [".toByteArray())
                res.filter {
                    when (it) {
                        is CellInfoGsm -> {
                            return@filter it.cellIdentity.cid != 0
                        }
                        is CellInfoCdma -> {
                            return@filter it.cellIdentity.basestationId != 0
                        }
                        is CellInfoLte -> {
                            return@filter it.cellIdentity.ci != 0
                        }
                        is CellInfoWcdma -> {
                            return@filter it.cellIdentity.cid != 0
                        }
                        else -> {
                            return@filter false
                        }
                    }
                }.forEachIndexed {i, it ->
                    when (it) {
                        is CellInfoGsm -> {
                            conn.getOutputStream().write(
                                "{\"radioType\":\"gsm\",\"cellId\": ${it.cellIdentity.cid}, \"locationAreaCode\": ${it.cellIdentity.lac}, \"mobileCountryCode\": ${it.cellIdentity.mcc}, \"mobileNetworkCode\": ${it.cellIdentity.mnc}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
                            )
                        }
                        is CellInfoCdma -> {
                            conn.getOutputStream().write(
                                "{\"radioType\":\"cdma\",\"cellId\": ${it.cellIdentity.basestationId}, \"locationAreaCode\": ${it.cellIdentity.networkId}, \"mobileCountryCode\": ${it.cellIdentity.systemId}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
                            )
                        }
                        is CellInfoLte -> {
                            conn.getOutputStream().write(
                                "{\"radioType\":\"lte\",\"cellId\": ${it.cellIdentity.ci shl 8}, \"locationAreaCode\": ${it.cellIdentity.tac}, \"mobileCountryCode\": ${it.cellIdentity.mcc}, \"mobileNetworkCode\": ${it.cellIdentity.mnc}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
                            )
                        }
//                        is CellInfoNr -> {
//                            conn.getOutputStream().write(
//                                "{\"cellId\": ${it.cellIdentity.nci}, \"locationAreaCode\": ${it.cellIdentity.tac}, \"mobileCountryCode\": ${it.cellIdentity.mccString}, \"mobileNetworkCode\": ${it.cellIdentity.mncString}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
//                            )
//                        }
//                        is CellInfoTdscdma -> {
//                            conn.getOutputStream().write(
//                                "{\"cellId\": ${it.cellIdentity.cid}, \"locationAreaCode\": ${it.cellIdentity.lac}, \"mobileCountryCode\": ${it.cellIdentity.mcc}, \"mobileNetworkCode\": ${it.cellIdentity.mnc}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
//                            )
//                        }
                        is CellInfoWcdma -> {
                            conn.getOutputStream().write(
                                "{\"radioType\":\"wcdma\",\"cellId\": ${it.cellIdentity.cid}, \"locationAreaCode\": ${it.cellIdentity.lac}, \"mobileCountryCode\": ${it.cellIdentity.mcc}, \"mobileNetworkCode\": ${it.cellIdentity.mnc}, \"signalStrength\": ${it.cellSignalStrength.dbm}}".toByteArray()
                            )
                        }
                    }
                    if (i < res.size - 1) {
                        conn.getOutputStream().write(",".toByteArray())
                    }
                }
                conn.getOutputStream().write("]}".toByteArray())
                if ((conn as HttpURLConnection).responseCode != 200) {
                    Log.e(TAG, "getGeoLocationFromCellTowers: ${conn.responseCode} ${conn.responseMessage}  ${conn.errorStream?.bufferedReader()?.readText()}")
                    return null
                }
                val loc = conn.getInputStream().bufferedReader().readText()
                val lat = loc.substringAfter("lat\":").substringBefore(",")
                val lon = loc.substringAfter("lng\":").substringBefore("}")
                val acc =
                    loc.substringAfter("accuracy\":").substringBefore(",").substringBefore("}")
                location = Location("Cell").apply {
                    latitude = lat.toDouble()
                    longitude = lon.toDouble()
                    accuracy = acc.toFloat() / 100.0f
                }
            } catch (e: Exception) {
                Log.e(TAG, "getMyLocation: ", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            return location
        }
    }
}