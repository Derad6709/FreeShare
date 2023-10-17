package com.a.freeshare.fragment.connection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.Session
import com.a.freeshare.SocketTransferService
import com.a.freeshare.activity.SelectActivity
import com.a.freeshare.activity.SessionActivity
import com.a.freeshare.adapter.ShareRecyclerViewAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.ConnectionImpl
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.obj.HelperItem
import com.google.android.material.appbar.MaterialToolbar
import java.lang.NullPointerException

@SuppressLint("MissingPermission")
class TransferFragment:BaseFragment(),ConnectionImpl {

    class NonPredictiveLinearLayoutManager(context:Context):LinearLayoutManager(context){
        override fun supportsPredictiveItemAnimations(): Boolean {
            return false
        }
    }

    class LoadingDialogFragment:DialogFragment(){

        companion object
        {
             const val TAG = "TransferFragment"
        }

        init {

            isCancelable = false
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_loading_dialog,container,false)
        }
    }

    companion object{
        val TAG = TransferFragment::class.simpleName

        private const val HELPERS = "helpers"
        private const val CURRENT_BY_TOTAL = "current_by_total"
        private const val CURRENT_TOTAL = "current_total"
        private const val CURRENT = "current"
        private const val VISIBILITY = "visibility"

        private const val TRANSFER_VIEW_TYPE_SEND = 0
        private const val TRANSFER_VIEW_TYPE_RECEIVE = 1
    }

    private val manager by lazy {
        (requireActivity() as SessionActivity).getP2pManager()
    }
    private val channel by lazy {
        (requireActivity() as SessionActivity).getP2pChannel()
    }

    private var currentByTotal:String = "0/0"
    private var currentTotal:Int = 0
    private var current:Int = 0

    private var visibility = View.VISIBLE
    private var removeGroup = false

    private var itemsToSend:ArrayList<FileItem>? = null

    private lateinit var transferHandler:Handler

    private lateinit var helperItems : ArrayList<HelperItem>

    private lateinit var wifiP2pInfo: WifiP2pInfo

    private lateinit var shareRecyclerView: RecyclerView

    private lateinit var adapter: ShareRecyclerViewAdapter

    private lateinit var service: SocketTransferService

    private lateinit var layoutExtraActionHolder:ConstraintLayout
    private lateinit var btnSendMore:Button
    private lateinit var btnDisconnect:Button

    private lateinit var txtItemsOf:TextView

    private var shouldClose:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transferHandler = Handler(Looper.getMainLooper())

        initGlobalVariables(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transfer,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {

            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        initViews(view)

        startServiceAndBind(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.con)
        ) { v, insets ->
            val l = v.layoutParams as MarginLayoutParams
            val i = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            l.leftMargin = i.left
            l.topMargin = i.top
            l.rightMargin = i.right
            l.bottomMargin = i.bottom

            v.layoutParams = l

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

          if (requestCode == 101 && resultCode == Activity.RESULT_OK){

              itemsToSend = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  data?.getSerializableExtra(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
              } else {
                  data?.getSerializableExtra(ITEMS) as ArrayList<FileItem>
              }

              service.sendReceiveCommand(itemsToSend!!)

          }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        itemsToSend?.also {
            outState.putParcelableArrayList(ITEMS,itemsToSend)
        }
        outState.putParcelable(P2P_INFO,wifiP2pInfo)
        outState.putParcelableArrayList(HELPERS,helperItems)
        outState.putString(CURRENT_BY_TOTAL,txtItemsOf.text.toString())
        outState.putInt(CURRENT_TOTAL,currentTotal)
        outState.putInt(VISIBILITY,layoutExtraActionHolder.visibility)
        outState.putInt(CURRENT,current)
    }

    override fun hasCleared(): Boolean {
        return shouldClose
    }

    private fun initShareRecycler(service: SocketTransferService) {
        adapter = ShareRecyclerViewAdapter(helperItems,service,null)

        shareRecyclerView.adapter = adapter
        shareRecyclerView.layoutManager = NonPredictiveLinearLayoutManager(requireActivity())

    }

    private fun initGlobalVariables(savedInstanceState: Bundle?){

        helperItems = if (savedInstanceState == null){
            ArrayList()
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                savedInstanceState.getParcelableArrayList(HELPERS,ArrayList::class.java) as ArrayList<HelperItem>
            }else{
                savedInstanceState.getParcelableArrayList<HelperItem>(HELPERS)!!
            }
        }

        wifiP2pInfo = if (savedInstanceState == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                arguments?.getParcelable(P2P_INFO,WifiP2pInfo::class.java) as WifiP2pInfo
            }else{
                arguments?.getParcelable(P2P_INFO)!!
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                savedInstanceState.getParcelable(P2P_INFO,WifiP2pInfo::class.java) as WifiP2pInfo
            }else{
                savedInstanceState.getParcelable(P2P_INFO)!!
            }
        }


        if (savedInstanceState!=null){
            currentByTotal = savedInstanceState.getString(CURRENT_BY_TOTAL)!!
            currentTotal = savedInstanceState.getInt(CURRENT_TOTAL)
            current = savedInstanceState.getInt(CURRENT)
            visibility = savedInstanceState.getInt(VISIBILITY)
        }

        try {

            itemsToSend = if (savedInstanceState != null){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelableArrayList(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                }else{
                    savedInstanceState.getParcelableArrayList<Parcelable>(ITEMS) as ArrayList<FileItem>
                }
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getParcelableArrayList(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                }else{
                    arguments?.getParcelableArrayList<Parcelable>(ITEMS) as ArrayList<FileItem>
                }
            }


        }catch (npe:NullPointerException){
            Log.d(TAG,"sharing is receive")
        }

    }

    private fun initViews(view: View){

        txtItemsOf = view.findViewById<TextView?>(R.id.fragment_transfer_items_of).apply {
            text = currentByTotal
        }

        shareRecyclerView = view.findViewById(R.id.share_recycler)

        layoutExtraActionHolder = view.findViewById<ConstraintLayout?>(R.id.con).apply {
            visibility = this@TransferFragment.visibility
        }

        btnSendMore = view.findViewById<Button?>(R.id.fragment_transfer_send_more).apply {

            setOnClickListener {
                val selectIntent = Intent(requireActivity(),SelectActivity::class.java)
                selectIntent.action = SelectActivity.ACTION_SELECT
                startActivityForResult(selectIntent,101)
            }
        }

        btnDisconnect = view.findViewById<Button?>(R.id.fragment_transfer_disconnect).apply {

            setOnClickListener {

                removeGroup = true
                service.closeSession()
            }
        }
    }

    private fun startServiceAndBind(savedInstanceState: Bundle?){

        val serviceIntent = Intent(requireActivity(), SocketTransferService::class.java)

        if (!wifiP2pInfo.isGroupOwner)serviceIntent.putExtra(SocketTransferService.EXTRA_HOST,wifiP2pInfo.groupOwnerAddress.hostAddress)

        if (savedInstanceState == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(serviceIntent)
            }else{
                requireActivity().startService(serviceIntent)
            }
        }

        requireActivity().bindService(serviceIntent,object : ServiceConnection {

            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                val binder = p1 as SocketTransferService.InnerBinder
                val service = binder.getService()

                Log.d("SocketService","service bound")

                this@TransferFragment.service = service

                    initShareRecycler(service)

                    setServiceCallbacks(service)

                    if (savedInstanceState == null) {
                        LoadingDialogFragment().show(requireActivity().supportFragmentManager,LoadingDialogFragment.TAG)
                        service.startSession()
                    }

            }

            override fun onServiceDisconnected(p0: ComponentName?) {

            }
        },Context.BIND_AUTO_CREATE)

    }

    private fun setServiceCallbacks(service: SocketTransferService){

        val sentString = requireActivity().getString(R.string.sent)
        val receivedString = requireActivity().getString(R.string.received)

        val sharedPreferences = context?.getSharedPreferences("History",Context.MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()

        val socketListener = object :SocketListener{

            override fun onSocket() {

                shouldClose = false

                if (itemsToSend != null)service.send(itemsToSend!!.toList())else service.receive()

                transferHandler.post {
                    requireActivity().supportFragmentManager.findFragmentByTag(LoadingDialogFragment.TAG)?.also {

                        if (it is LoadingDialogFragment)it.dismiss()
                    }
                }
            }

            override fun onSocketClosed() {

                editor.apply()
                editor.commit()
                if (removeGroup)disconnectWifiP2p()

                shouldClose = true

                transferHandler.post {

                    layoutExtraActionHolder.visibility = View.GONE

                }
            }
        }

        val sessionImpl = object :Session.SessionImpl{

            override fun onStarted() {
                
                transferHandler.post {
                    layoutExtraActionHolder.visibility = View.GONE
                }
            }

            override fun onEnded() {

                transferHandler.post {
                    layoutExtraActionHolder.visibility = View.VISIBLE
                }
            }
        }

        val transferImpl = object :TransferImpl{

            override fun onSendFiles(adapterPosition: Int, count: Int, files: List<FileItem>) {
                for (item in files){

                    helperItems.add(HelperItem(item.name,item.absPath,item.content,item.mime,item.dataSize,HelperItem.ItemState.ENQUEUED,HelperItem.ItemState.SENT))
                }
                transferHandler.post {

                        current = 0
                        currentTotal = count
                        txtItemsOf.text = "$sentString $current/$currentTotal"
                        adapter.notifyItemRangeInserted(adapterPosition,count)
                }

            }

            override fun onStartSend(index: Int, name: String, absPath: String?,mime:String?, length: Long) {

                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.STARTED
                    it.name = name
                    it.absPath = absPath
                    it.mime = mime
                    it.maxValue = length
                }
                transferHandler.post {

                    adapter.notifyItemChanged(index)
                    shareRecyclerView.scrollToPosition(index)
                }
            }

            override fun onBytesSent(index: Int, bytes: Long) {

                helperItems[index].also {
                    it.currentValue+=bytes
                    it.itemState = HelperItem.ItemState.IN_PROGRESS
                }

            }

            override fun onEndSend(index: Int) {
                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.ENDED
                }

                   editor.putString(helperItems[index].name,helperItems[index].absPath)
                    transferHandler.post {

                        current++
                        txtItemsOf.text = "$sentString $current/$currentTotal"
                        adapter.notifyItemChanged(index)
                    }
            }

            override fun onReceiveFiles(adapterPosition: Int, count: Int, queuedNames: Array<String?>) {

                for (i in 0 until count){
                    helperItems.add(HelperItem(queuedNames[i],null,null,null,null,HelperItem.ItemState.ENQUEUED,HelperItem.ItemState.RECEIVED))
                }

                transferHandler.post {

                    current = 0
                    currentTotal = count
                    txtItemsOf.text = "$receivedString $current/$currentTotal"
                    adapter.notifyItemRangeInserted(adapterPosition,count)
                }

            }

            override fun onStartReceive(index: Int, name: String, absPath: String?,mime:String?, length: Long) {

                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.STARTED
                    it.name = name
                    it.absPath = absPath
                    it.mime = mime
                    it.maxValue = length
                }

                transferHandler.post {

                    adapter.notifyItemChanged(index)
                    shareRecyclerView.scrollToPosition(index)
                }
            }

            override fun onBytesReceived(index: Int, bytes: Long) {
                    helperItems[index].also {
                        it.currentValue+=bytes
                        it.itemState = HelperItem.ItemState.IN_PROGRESS
                    }
            }

            override fun onEndReceive(index: Int) {
                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.ENDED
                }

                editor.putString(helperItems[index].name,helperItems[index].absPath)
                    transferHandler.post {
                        current++
                        txtItemsOf.text = "$receivedString $current/$currentTotal"
                        adapter.notifyItemChanged(index)
                    }
            }

            override fun onSkipped(index: Int) {

                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.SKIPPED
                }
                transferHandler.post {
                    adapter.notifyItemChanged(index)
                }
            }
        }

        service.setSocketListener(socketListener)
        service.setTransferImpl(transferImpl)
        service.setSessionImpl(sessionImpl)
    }


    private fun disconnectWifiP2p(){

                manager.requestGroupInfo(channel){groupInfo->

                    if (groupInfo != null){
                        manager.removeGroup(channel,object :WifiP2pManager.ActionListener{

                            override fun onSuccess() {
                                Toast.makeText(requireActivity(),"closing p2p as owner",Toast.LENGTH_SHORT).show()
                            }

                            override fun onFailure(p0: Int) {
                                Toast.makeText(requireActivity(),"closing failed as owner",Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
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
        if (!wifiP2pInfo.groupFormed) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onChannelDisconnected() {

    }
}