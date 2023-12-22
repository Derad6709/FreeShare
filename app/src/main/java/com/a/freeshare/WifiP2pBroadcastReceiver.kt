package com.a.freeshare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.obj.FileItem
import android.annotation.SuppressList

@SuppressLint("MissingPermission")
class WifiP2pBroadcastReceiver() : BroadcastReceiver() {


    private lateinit var p2pManager:WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private  var callback: ConnectionImpl? = null

    var discoveryState = WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
    var p2pState = WifiP2pManager.WIFI_P2P_STATE_DISABLED

    constructor(p2pManager: WifiP2pManager,channel: WifiP2pManager.Channel) : this() {
        this.p2pManager = p2pManager
        this.channel = channel
    }

    override fun onReceive(context: Context, intent: Intent) {

        when(intent.action){

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION->{
                intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,WifiP2pManager.WIFI_P2P_STATE_DISABLED).also {

                        p2pState = it
                        callback?.onWifiP2pState(it == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION->{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    p2pManager.requestDeviceInfo(channel){
                        callback?.onThisDeviceChanged(it!!)
                    }
                }else{
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)?.also {
                        callback?.onThisDeviceChanged(it)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION->{
                p2pManager.requestPeers(channel){
                    callback?.onWifiDeviceListChanged(it)
                }
            }

            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION->{
                intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED).also {
                    if (it != discoveryState){
                        callback?.onWifiP2pDiscoveryChanged(it == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION->{
                p2pManager.requestConnectionInfo(channel){
                    callback?.onWifiP2pConnection(it)
                }
            }
        }
    }

    fun setConnectionCallback(connectionImpl: ConnectionImpl){
        this.callback = connectionImpl
    }
}
