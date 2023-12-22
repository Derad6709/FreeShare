package com.a.freeshare.fragment.connection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.set
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.SocketTransferService
import com.a.freeshare.activity.SessionActivity
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.WirelessStateWrapper
import com.a.rippleview.RippleView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.lang.Exception
import java.net.Inet4Address
import java.net.NetworkInterface

@SuppressLint("MissingPermission")
class HostDeviceFragment: BaseFragment(),ConnectionImpl {


    private lateinit var items:ArrayList<FileItem>

    private lateinit var rippleView: RippleView

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
    private var deviceName:String? = null

    fun handleItems(ii:ArrayList<FileItem>){

        for (i in items){

            for (t in ii){
                if (i.name.equals(t.name)){
                    ii.remove(t)
                }
            }
        }

        items.addAll(ii)

        Toast.makeText(requireActivity(),items.size.toString(),Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statesWrapper = WirelessStateWrapper(requireActivity())

        items = arrayListOf()

        items.addAll(

            if (savedInstanceState != null){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelableArrayList(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                } else {
                    savedInstanceState.getParcelableArrayList<Parcelable>(ITEMS) as ArrayList<FileItem>
                }
            }else{

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getParcelableArrayList(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                } else {
                    arguments?.getParcelableArrayList<Parcelable>(ITEMS) as ArrayList<FileItem>
                }
            }
        )

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

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {

            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        rippleView = view.findViewById(R.id.rippleView)
        rippleView.setRippleTime(5000)
        rippleView.setConsecutiveDelay(1000)

        manager.requestGroupInfo(channel){

            if (it != null){
                manager.removeGroup(channel,null)
            }else{
                if(p2pReceiver.discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED && deviceName != null)discoverPeers()
            }
        }


    }

    override fun onStart() {
        super.onStart()
        //rippleView.startRipple()
    }

    override fun onStop() {
        super.onStop()
        rippleView.endRipple()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(ITEMS,items)

    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice) {
        view?.findViewById<TextView>(R.id.deviceName)?.apply {

            deviceName = device.deviceName
            text = device.deviceName


            view?.findViewById<ImageView>(R.id.qrGen)?.apply {
                val bitMat = MultiFormatWriter().encode(deviceName!!,BarcodeFormat.QR_CODE,measuredWidth,measuredHeight)

                val qrBit = Bitmap.createBitmap(bitMat.width,bitMat.height,Bitmap.Config.ARGB_8888)

                for (i in 0 until bitMat.width){
                    for (j in 0 until bitMat.height){
                        qrBit[i, j] = if (bitMat.get(i,j) )Color.BLACK else Color.WHITE
                    }
                }

                setImageBitmap(qrBit)
            }
        }
    }

    override fun onWifiP2pState(enabled: Boolean) {

        if (enabled){
            discoverPeers()

        }else{
            rippleView.endRipple()
        }
    }

    override fun onWifiP2pDiscoveryChanged(discovering: Boolean) {
        if (discovering){
            rippleView.startRipple()
        } else {

            rippleView.endRipple()

        }
    }

    override fun onWifiDeviceListChanged(deviceList: WifiP2pDeviceList) {

    }

    override fun onWifiP2pConnection(wifiP2pInfo: WifiP2pInfo) {

        if (wifiP2pInfo.groupFormed){

            val transferFragment = TransferFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(P2P_INFO,wifiP2pInfo)
                    putSerializable(ITEMS,items)
                }
            }

            requireActivity().supportFragmentManager.commit {
                replace(R.id.activity_session_container,transferFragment,BaseFragment.TAG)
            }
        }
    }

    override fun onChannelDisconnected() {

    }


    private fun discoverPeers(){

        manager.discoverPeers(channel,object :WifiP2pManager.ActionListener{

            override fun onSuccess() {
                Log.i(FindDeviceFragment.TAG,"discover command sent success")
            }

            override fun onFailure(p0: Int) {
                Log.e(FindDeviceFragment.TAG,"discover command failed : $p0")

                snackIfDiscoverError()
            }
        })
    }

    private fun snackIfDiscoverError(){

        if (p2pReceiver.p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED && !statesWrapper.getLocationState())
        {
            Snackbar.make(requireContext(),requireView(),"Location access ERROR!", Snackbar.LENGTH_LONG)
                .setAction("Enable") {
                    statesWrapper.tryEnableLocation()
                }.show()
        }
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
}
