package com.a.freeshare.fragment.connection

import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.FloatRange
import androidx.annotation.NonNull
import com.a.freeshare.R
import com.a.freeshare.activity.SessionActivity
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.ConnectionImpl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar

class FindDeviceFragment : BaseFragment(), ConnectionImpl {

    companion object {
        const val DEVICE_LIST = "device_list"
    }

    private lateinit var deviceNames: ArrayList<String>
    private var devices: WifiP2pDeviceList? = null
    private lateinit var dAdapter: ArrayAdapter<String>
    private lateinit var devicesListView: ListView
    private lateinit var scanProgress: LinearProgressIndicator
    private lateinit var btnRescan: Button

    private val manager by lazy {
        (requireActivity() as SessionActivity).getP2pManager()
    }
    private val channel by lazy {
        (requireActivity() as SessionActivity).getP2pChannel()
    }

    private val wifiManager by lazy{
        requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val p2pReceiver by lazy {
        (requireActivity() as SessionActivity).getP2pReceiver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_find_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        devicesListView = view.findViewById(R.id.fragment_find_device_device_list)

        scanProgress = view.findViewById<LinearProgressIndicator?>(R.id.fragment_find_device_device_linear_progress_ind).apply {
            visibility = if (p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) View.VISIBLE else View.GONE
        }

        btnRescan = view.findViewById<Button?>(R.id.fragment_find_device_rescan).apply {
            setOnClickListener {

                 if (p2pReceiver.p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED && p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){

                     manager.discoverPeers(channel,object :WifiP2pManager.ActionListener{

                         override fun onSuccess() {
                             Log.i(TAG,"discover command sent success")
                         }

                         override fun onFailure(p0: Int) {
                             Log.e(TAG,"discover command failed : $p0")
                         }
                     })
                 }else if (p2pReceiver.p2pState == WifiP2pManager.WIFI_P2P_STATE_DISABLED){
                     showSnackBarAboutWifi()
                 }
            }
        }

        if (savedInstanceState == null) {
            deviceNames = ArrayList()
            dAdapter = ArrayAdapter(
                requireActivity(), android.R.layout.simple_list_item_1,
                deviceNames
            )
        } else {

            deviceNames = ArrayList()
            try {
                devices = if (Build.VERSION.SDK_INT >= 33) {
                    savedInstanceState.getParcelable(DEVICE_LIST, WifiP2pDeviceList::class.java)
                } else {
                    savedInstanceState.getParcelable(DEVICE_LIST)!!
                }

                deviceNames.apply {
                    for (d in devices!!.deviceList) {
                        add(d.deviceName)
                    }
                }
            } catch (npe: NullPointerException) {

            }

            dAdapter = ArrayAdapter(
                requireActivity(), android.R.layout.simple_list_item_1,
                deviceNames
            )
        }

        devicesListView.apply {
            adapter = dAdapter

            onItemClickListener = object : AdapterView.OnItemClickListener {
                override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

                    showConfirmConnectDialog(p2)
                }
            }

            onItemLongClickListener = object : AdapterView.OnItemLongClickListener {

                override fun onItemLongClick(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ): Boolean {

                    val popUp = PopupMenu(requireActivity(), p1)
                    popUp.menuInflater.inflate(R.menu.connect_menu, popUp.menu)
                    popUp.setOnMenuItemClickListener {
                        if (it.itemId == R.id.connect_menu_connect) {
                            showConfirmConnectDialog(p2)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popUp.show()

                    return true
                }
            }
        }

        if(p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)manager.discoverPeers(channel,object :WifiP2pManager.ActionListener{

            override fun onFailure(p0: Int) {

            }

            override fun onSuccess() {
                Log.d(TAG,"discover command send")
            }
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        devices?.also {
            outState.putParcelable(DEVICE_LIST, it)
        }
    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice) {

    }

    override fun onWifiP2pState(enabled: Boolean) {

        if (!enabled){
              showSnackBarAboutWifi()

          }else if (enabled && p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){

              manager.discoverPeers(channel,object :WifiP2pManager.ActionListener{

                  override fun onFailure(p0: Int) {

                  }

                  override fun onSuccess() {
                      Log.d(TAG,"discover command send")
                  }
              })
          }
    }

    override fun onWifiP2pDiscoveryChanged(discovering: Boolean) {

          scanProgress.visibility = if (discovering)View.VISIBLE else View.GONE
    }

    override fun onWifiDeviceListChanged(deviceList: WifiP2pDeviceList) {

        deviceNames.clear()
        devices = deviceList
        for (d in deviceList.deviceList) {
            deviceNames.add(d.deviceName)
        }

        dAdapter.notifyDataSetChanged()
    }

    override fun onWifiP2pConnection(wifiP2pInfo: WifiP2pInfo) {

    }

    override fun onChannelDisconnected() {

    }

    private fun showConfirmConnectDialog(deviceIndex: Int) {

        val dialog = MaterialAlertDialogBuilder(requireActivity())
        dialog.setTitle(R.string.confirm)
        dialog.setMessage("${getString(R.string.connect_to)} ${deviceNames.get(deviceIndex)}")
        dialog.setPositiveButton(R.string.connect, object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface?, p1: Int) {

                connect(devices!!.deviceList.toMutableList().get(deviceIndex))
            }
        })
        dialog.show()
    }

    private fun connect(device: WifiP2pDevice) {

        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceName

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onFailure(p0: Int) {
                Log.e(TAG, "connect command failed : $p0")
            }

            override fun onSuccess() {
                Log.d(TAG, "connection init")
            }

        })
    }

    private fun showSnackBarAboutWifi(){
        val snack = Snackbar.make(requireContext(),requireView(),"Mak sure wifi is turned on",Snackbar.LENGTH_LONG)
        snack.setAction("Turn on",View.OnClickListener {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                 val wifiPanelIntent = Intent(Settings.Panel.ACTION_WIFI)
                 startActivity(wifiPanelIntent)
             }else{

                wifiManager.isWifiEnabled = true
             }
        })
        snack.show()
    }

    private fun viewScaleAnimate(
        @NonNull v: View,
        @FloatRange(0.0, 1.0) scaleStart: Float,
        @FloatRange(0.0, 1.0) scaleEnd: Float
    ) {
        ValueAnimator.ofFloat(scaleStart, scaleEnd).apply {
            duration = 500L
            addUpdateListener {
                val value = it.animatedValue as Float
                v.scaleX = value
                v.scaleY = value
            }
            start()
        }
    }
}