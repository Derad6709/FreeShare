package com.a.freeshare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import com.a.freeshare.impl.OnStateChangedListener

class LocationBroadcastReceiver:BroadcastReceiver {

    constructor():super()
    constructor(stateChangedListener: OnStateChangedListener){
        this.stateChangedListener = stateChangedListener
    }
    private var stateChangedListener:OnStateChangedListener? = null

    override fun onReceive(p0: Context?, p1: Intent?) {

        if (p1!!.action == LocationManager.PROVIDERS_CHANGED_ACTION){

            (p0!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER).also {
                stateChangedListener?.onStateChanged(if (it)1 else 0)
            }
        }
    }
}