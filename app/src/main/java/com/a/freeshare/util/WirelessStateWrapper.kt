package com.a.freeshare.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.ContextThemeWrapper

class WirelessStateWrapper(private val ctx:Context){

    private lateinit var wifiManager :WifiManager
    private val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    }

    fun tryEnableWifi(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                val wifiPanelIntent = Intent(Settings.Panel.ACTION_WIFI)
                ctx.startActivity(wifiPanelIntent)
        }else{
            wifiManager.isWifiEnabled = true
        }
    }

    fun getWifiState() = wifiManager.wifiState

    @SuppressLint("MissingPermission")
    fun tryEnableBluetooth(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

        }else{
            bluetoothManager.adapter.enable()
        }
    }

    fun getBluetoothState() = bluetoothManager.adapter.state

    fun tryEnableLocation(){
        ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun getLocationState() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}