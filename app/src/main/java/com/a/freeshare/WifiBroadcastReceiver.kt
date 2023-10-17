package com.a.freeshare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import com.a.freeshare.impl.OnStateChangedListener

class WifiBroadcastReceiver:BroadcastReceiver{

    constructor():super()


    private var stateChangeListener: OnStateChangedListener? = null

    constructor(stateChangedListener: OnStateChangedListener):super(){
        this.stateChangeListener = stateChangedListener
    }

    override fun onReceive(p0: Context?, p1: Intent?) {

        val action = p1!!.action

        if (action == WifiManager.WIFI_STATE_CHANGED_ACTION){

            stateChangeListener?.onStateChanged(p1.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_DISABLED))
        }
    }
}