package com.a.freeshare.impl

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager

interface ConnectionImpl : WifiP2pManager.ChannelListener {

    fun onThisDeviceChanged(device:WifiP2pDevice)
    fun onWifiP2pState(enabled:Boolean)
    fun onWifiP2pDiscoveryChanged(discovering:Boolean)
    fun onWifiDeviceListChanged(deviceList:WifiP2pDeviceList)
    fun onWifiP2pConnection(wifiP2pInfo: WifiP2pInfo)
    override fun onChannelDisconnected()
}