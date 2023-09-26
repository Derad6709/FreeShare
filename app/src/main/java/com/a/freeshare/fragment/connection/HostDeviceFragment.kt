package com.a.freeshare.fragment.connection

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a.freeshare.R
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.obj.FileItem

class HostDeviceFragment: BaseFragment(),ConnectionImpl {


    private lateinit var items:ArrayList<FileItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_host_device,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(ITEMS,items)

    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice) {

    }

    override fun onWifiP2pState(enabled: Boolean) {

    }

    override fun onWifiP2pDiscoveryChanged(discovering: Boolean) {

    }

    override fun onWifiDeviceListChanged(deviceList: WifiP2pDeviceList) {

    }

    override fun onWifiP2pConnection(wifiP2pInfo: WifiP2pInfo) {

    }

    override fun onChannelDisconnected() {

    }
}