package com.a.freeshare.fragment.connection

import android.annotation.SuppressLint
import android.content.*
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.activity.SessionActivity
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.util.WirelessStateWrapper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView

@SuppressLint("MissingPermission")
class FindDeviceFragment : BaseFragment(), ConnectionImpl {

    companion object {
        const val DEVICE_LIST = "device_list"
        val TAG = FindDeviceFragment::class.simpleName
    }

    private lateinit var deviceNames: ArrayList<String>
    private var devices: WifiP2pDeviceList? = null
    private lateinit var dAdapter: ArrayAdapter<String>
    private lateinit var devicesListView: ListView
    private lateinit var scanProgress: LinearProgressIndicator
    private lateinit var btnRescan: Button
    private lateinit var squareBarcodeView:BarcodeView

    private var deviceName: String? = null
    private var connecting:Boolean = false

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

    private lateinit var statesWrapper: WirelessStateWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statesWrapper = WirelessStateWrapper(requireActivity())
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

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {

            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        devicesListView = view.findViewById(R.id.fragment_find_device_device_list)

        scanProgress = view.findViewById<LinearProgressIndicator?>(R.id.fragment_find_device_device_linear_progress_ind).apply {
            visibility = if (p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) View.VISIBLE else View.GONE
        }

        btnRescan = view.findViewById<Button?>(R.id.fragment_find_device_rescan).apply {
            setOnClickListener {

                 if (p2pReceiver.p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED && p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){

                     discoverPeers()
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
               Log.e(TAG,"last devices are null")
            }

            dAdapter = ArrayAdapter(
                requireActivity(), android.R.layout.simple_list_item_1,
                deviceNames
            )
        }

        devicesListView.apply {
            adapter = dAdapter

            onItemClickListener = AdapterView.OnItemClickListener { p0, p1, p2, p3 -> showConfirmConnectDialog(p2) }

            /*onItemLongClickListener = object : AdapterView.OnItemLongClickListener {

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
            }*/
        }

        manager.requestGroupInfo(channel){

            if (it != null){
                manager.removeGroup(channel,null)
            }else{
                if(p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)discoverPeers()
            }
        }

       squareBarcodeView =  view.findViewById<BarcodeView>(R.id.barcodeView).apply {

            decodeSingle(object :BarcodeCallback{

                override fun barcodeResult(result: BarcodeResult?) {
                    result?.also {
                        deviceName = it.text
                        Toast.makeText(requireActivity(),it.text,Toast.LENGTH_SHORT).show()
                        findAndConnect()
                        pause()
                        stopDecoding()
                    }
                }
            })
        }

    }

    override fun onResume() {
        super.onResume()
        squareBarcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
       squareBarcodeView.pause()
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
              scanProgress.visibility = View.GONE
          }else if (enabled && p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){

              discoverPeers()
          }
    }

    override fun onWifiP2pDiscoveryChanged(discovering: Boolean) {

          scanProgress.visibility = if (discovering)View.VISIBLE else View.GONE

    }

    override fun onWifiDeviceListChanged(deviceList: WifiP2pDeviceList) {

        deviceNames.clear()

               devices = deviceList

                   var connectDevice:WifiP2pDevice? = null

                   for (d in deviceList.deviceList) {
                       deviceNames.add(d.deviceName)

                       if(deviceName != null && deviceName.equals(d.deviceName) && !connecting)connectDevice = d

                   }

                   dAdapter.notifyDataSetChanged()

                   if (connectDevice != null)showConfirmConnectDialog(devices!!.deviceList.indexOf(connectDevice))


    }

    override fun onWifiP2pConnection(wifiP2pInfo: WifiP2pInfo) {

            if (wifiP2pInfo.groupFormed){

                val transferFragment = TransferFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(P2P_INFO,wifiP2pInfo)
                    }
                }

                requireActivity().supportFragmentManager.commit {
                    replace(R.id.activity_session_container,transferFragment,BaseFragment.TAG)
                }
            }
    }

    override fun onChannelDisconnected() {

    }

    private fun showConfirmConnectDialog(deviceIndex: Int) {

        val dialog = MaterialAlertDialogBuilder(requireActivity())
        dialog.setTitle(R.string.confirm)
        dialog.setMessage("${getString(R.string.connect_to)} ${deviceNames.get(deviceIndex)}")
        dialog.setPositiveButton(R.string.connect) { p0, p1 -> connect(devices!!.deviceList.toMutableList()[deviceIndex]) }
        dialog.show()

    }


    private fun connect(device: WifiP2pDevice) {

        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        config.groupOwnerIntent = WifiP2pConfig.GROUP_OWNER_INTENT_MAX

        Toast.makeText(requireActivity(),"trying connect to ${device.deviceName},${device.deviceAddress}",Toast.LENGTH_SHORT).show()

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onFailure(p0: Int) {
                Log.e(TAG, "connect command failed : $p0")
                Toast.makeText(requireActivity(),"connection init failed",Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                connecting = true
                Log.d(TAG, "connection init")
                Toast.makeText(requireActivity(),"connection started",Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun showSnackBarAboutWifi(){

        val snack = Snackbar.make(requireActivity(),requireView(),"Mak sure wifi is turned on",Snackbar.LENGTH_LONG)
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

    private fun snackIfDiscoverError(){

        if (p2pReceiver.p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED && !statesWrapper.getLocationState())
        {
            Snackbar.make(requireContext(),requireView(),"Location access ERROR!",Snackbar.LENGTH_LONG)
                .setAction("Enable") {
                    statesWrapper.tryEnableLocation()
                }.show()
        }
    }

    private fun discoverPeers(){

        manager.discoverPeers(channel,object :WifiP2pManager.ActionListener{

            override fun onSuccess() {
                Log.i(TAG,"discover command sent success")
            }

            override fun onFailure(p0: Int) {
                Log.e(TAG,"discover command failed : $p0")

                snackIfDiscoverError()
            }
        })
    }

    private fun findAndConnect(){

        devices?.also {
            if (!it.deviceList.isEmpty()){

                var connectDevice:WifiP2pDevice? = null

                for (d in it.deviceList) {

                    if (deviceName != null && deviceName.equals(d.deviceName) && !connecting)connectDevice = d

                }

                if (connectDevice != null)showConfirmConnectDialog(it.deviceList.indexOf(connectDevice))
            }
        }
    }
}