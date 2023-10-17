package com.a.freeshare.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.WifiP2pBroadcastReceiver
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.fragment.connection.FindDeviceFragment
import com.a.freeshare.fragment.connection.HostDeviceFragment
import com.a.freeshare.fragment.connection.StatesResolveFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.UriUtil
import com.a.freeshare.util.WirelessStateWrapper
import com.google.android.material.internal.EdgeToEdgeUtils
import java.io.File
import java.lang.Exception
import java.net.FileNameMap
import java.net.URLConnection


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

    private  var sessionType:Int = SESSION_TYPE_RECEIVE
    private var items:ArrayList<FileItem>? = null
    private var p2pReceiver:WifiP2pBroadcastReceiver? =null
    private lateinit var p2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private var connectionFragment: BaseFragment? = null
    private lateinit var connectionCallbacks :ConnectionImpl

    private var gSavedInstanceState:Bundle? = null

    private val backPressedCallback = object : OnBackPressedCallback(true){

        override fun handleOnBackPressed() {

            if (connectionFragment == null){
                finish()
            }else if(connectionFragment!!.hasCleared()){
                finish()
            }
        }
    }

    private val fileMap:FileNameMap by lazy {
        URLConnection.getFileNameMap()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (sessionType == SESSION_TYPE_SEND){
            (connectionFragment as? HostDeviceFragment)?.handleItems(handleActionSendOrMultiple(intent))
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.applyEdgeToEdge(window,true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)
        gSavedInstanceState = savedInstanceState

        onBackPressedDispatcher.addCallback(this,backPressedCallback)

        initItems(savedInstanceState)

        val wsa = WirelessStateWrapper(this)
            val  allOK:Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                wsa.getWifiState() == WifiManager.WIFI_STATE_ENABLED
            }else{
                wsa.getWifiState() == WifiManager.WIFI_STATE_ENABLED && wsa.getLocationState()
            }

            if (!allOK) {
                if (savedInstanceState == null ){

                    val resolve = StatesResolveFragment()
                    if (sessionType == SESSION_TYPE_SEND){
                        resolve.arguments = Bundle().apply { putSerializable(BaseFragment.ITEMS,items) }
                    }

                    resolve.show(supportFragmentManager,StatesResolveFragment.TAG)
                }
            }else{

                init()

            }

    }

    private fun initItems(savedInstanceState: Bundle?){

        if (savedInstanceState == null){
            if(intent.action != null){
                if (intent.action == Intent.ACTION_SEND || intent.action ==Intent.ACTION_SEND_MULTIPLE){
                    handleActionSendOrMultiple(intent)?.also {
                        items = it
                    }

                    sessionType = SESSION_TYPE_SEND

                }
            }else{
                try {
                    items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(BaseFragment.ITEMS,Parcelable::class.java) as ArrayList<FileItem>
                    } else {
                        intent.getParcelableArrayListExtra<Parcelable>(BaseFragment.ITEMS) as ArrayList<FileItem>
                    }
                    sessionType = SESSION_TYPE_SEND

                }catch (e:Exception){
                    sessionType = SESSION_TYPE_RECEIVE

                    e.printStackTrace()
                }

            }
        }else{

            sessionType = savedInstanceState.getInt(SESSION_TYPE)

            if (sessionType == SESSION_TYPE_SEND){
                items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getSerializable(BaseFragment.ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                } else {
                    savedInstanceState.getSerializable(BaseFragment.ITEMS) as ArrayList<FileItem>
                }

            }
        }

    }

    fun init(){

        p2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = p2pManager.initialize(this, Looper.getMainLooper()
        ) {
            //onChannelDisconnected
        }

        p2pReceiver = WifiP2pBroadcastReceiver(p2pManager,channel)

        supportFragmentManager.addFragmentOnAttachListener { fragmentManager, fragment ->

            if (fragment is BaseFragment)connectionFragment = fragment
            if (fragment is ConnectionImpl)p2pReceiver!!.setConnectionCallback(connectionFragment as ConnectionImpl)

        }

        val checkFrag:Fragment? = supportFragmentManager.findFragmentByTag(BaseFragment.TAG)

        if (checkFrag == null){
            connectionFragment = if (sessionType == SESSION_TYPE_SEND) {
                HostDeviceFragment().apply {

                    arguments = Bundle().apply {
                        putParcelableArrayList(BaseFragment.ITEMS, items)
                    }
                }
            } else {
                FindDeviceFragment()
            }

            supportFragmentManager.commit { add(R.id.activity_session_container,connectionFragment!!,BaseFragment.TAG) }

        }else{
            connectionFragment = checkFrag as BaseFragment

        }

        if (connectionFragment is ConnectionImpl)p2pReceiver!!.setConnectionCallback(connectionFragment as ConnectionImpl)

        if (p2pReceiver != null)registerReceiver(p2pReceiver, p2pFilter)

    }

    private fun handleActionSendOrMultiple(pIntent: Intent?):ArrayList<FileItem>{

        val filePaths = ArrayList<FileItem>()

        if (pIntent!!.action != null){

            if (pIntent.action == Intent.ACTION_SEND){

                var parcel:Any = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    pIntent.getParcelableExtra(Intent.EXTRA_STREAM,Parcelable::class.java)

                }else{
                    pIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                }) as Parcelable

                if (parcel == null){
                    parcel = pIntent.data as Uri
                }

                val commonInfo = getCommonInfo(parcel as Uri)
                filePaths.add(FileItem(commonInfo.first,parcel as Uri,commonInfo.second,0,fileMap.getContentTypeFor(commonInfo.first)))

            }else if(pIntent.action == Intent.ACTION_SEND_MULTIPLE){

                var parcels:Any = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    pIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM,Parcelable::class.java)

                }else{
                    pIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }) as ArrayList<Parcelable>

                if (parcels == null){
                    parcels = pIntent.clipData as ClipData

                   for (i in 0 until parcels.itemCount){
                       val ps = parcels.getItemAt(i).uri
                       val commonInfo = getCommonInfo(ps as Uri)
                       filePaths.add(FileItem(commonInfo.first,ps as Uri,commonInfo.second,0,fileMap.getContentTypeFor(commonInfo.first)))
                   }

                }else{

                    for (ps in parcels as ArrayList<Parcelable>){
                        val commonInfo = getCommonInfo(ps as Uri)
                        filePaths.add(FileItem(commonInfo.first,ps as Uri,commonInfo.second,0,fileMap.getContentTypeFor(commonInfo.first)))

                    }
                }
            }
        }

        return filePaths
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //required by fragments
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (items != null) outState.putSerializable(BaseFragment.ITEMS,items)
        outState.putInt(SESSION_TYPE,sessionType)

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
        return p2pReceiver!!
    }

    override fun onResume() {
        super.onResume()
        if (p2pReceiver != null)registerReceiver(p2pReceiver, p2pFilter)
    }

    override fun onPause() {
        super.onPause()
        if (p2pReceiver != null)unregisterReceiver(p2pReceiver)
    }

    private fun getCommonInfo(uri: Uri):Pair<String,Long>{

        var name:String? = null
        var dataSize:Long? = 0

        try {
            val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)

            cursor?.also {
                if (it.moveToFirst()){
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    dataSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    it.close()
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

        return Pair(if (name == null) "N/A" else name,dataSize)
    }
}