package com.a.freeshare.activity

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.WifiP2pBroadcastReceiver
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.fragment.connection.FindDeviceFragment
import com.a.freeshare.fragment.connection.HostDeviceFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.obj.FileItem

class SessionActivity : AppCompatActivity() {

    companion object{
        val TAG = SessionActivity::class.simpleName

        const val SESSION_TYPE = "session_type"

        const val SESSION_TYPE_SEND = 1
        const val SESSION_TYPE_RECEIVE = 2

        val p2pFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
    }

    private  var sessionType:Int = -1
    private lateinit var items:ArrayList<FileItem>
    private lateinit var p2pReceiver:WifiP2pBroadcastReceiver
    private lateinit var p2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var connectionFragment: BaseFragment
    private lateinit var connectionCallbacks :ConnectionImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        sessionType = intent.getIntExtra(SESSION_TYPE,-1)


        if (savedInstanceState == null){

           connectionFragment = if (sessionType == SESSION_TYPE_SEND)HostDeviceFragment().apply {

               items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                   intent.getSerializableExtra(BaseFragment.ITEMS,ArrayList::class.java) as ArrayList<FileItem>
               } else {
                   intent.getSerializableExtra(BaseFragment.ITEMS) as ArrayList<FileItem>
               }

               arguments = Bundle().apply {
                   putSerializable(BaseFragment.ITEMS,items)
               }
           } else {
               FindDeviceFragment()
           }

            supportFragmentManager.commit { add(R.id.activity_session_container,connectionFragment,BaseFragment.TAG) }

        }else{
            connectionFragment = supportFragmentManager.findFragmentByTag(BaseFragment.TAG) as BaseFragment

        }

        if (connectionFragment is FindDeviceFragment || connectionFragment is HostDeviceFragment)connectionCallbacks = connectionFragment as ConnectionImpl

        p2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = p2pManager.initialize(this, Looper.getMainLooper(),connectionCallbacks)

        p2pReceiver = WifiP2pBroadcastReceiver(p2pManager,channel,connectionCallbacks)

    }

    @NonNull
    fun getP2pManager():WifiP2pManager{
        return p2pManager
    }

    @NonNull
    fun getP2pChannel():WifiP2pManager.Channel{
        return channel
    }

    @NonNull
    fun getP2pReceiver():WifiP2pBroadcastReceiver{
        return p2pReceiver
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(p2pReceiver, p2pFilter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(p2pReceiver)
    }
}