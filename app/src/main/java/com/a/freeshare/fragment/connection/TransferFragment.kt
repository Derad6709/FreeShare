package com.a.freeshare.fragment.connection

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.p2p.WifiP2pInfo
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.Session
import com.a.freeshare.SocketTransferService
import com.a.freeshare.activity.SelectActivity
import com.a.freeshare.adapter.ShareRecyclerViewAdapter
import com.a.freeshare.adapter.viewholder.AbsBaseHolder
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.obj.HelperItem
import com.a.freeshare.util.FileUtil
import java.lang.NullPointerException

class TransferFragment:BaseFragment() {

    class NonPredictiveLinearLayoutManager(context:Context):LinearLayoutManager(context){
        override fun supportsPredictiveItemAnimations(): Boolean {
            return false
        }
    }


    companion object{
        val TAG = TransferFragment::class.simpleName
        private const val SELECT_REQUEST_CODE = 1001
    }

    private var itemsToSend:ArrayList<FileItem>? = null

    private lateinit var handler:Handler

    private lateinit var helperItems : ArrayList<HelperItem>

    private lateinit var wifiP2pInfo: WifiP2pInfo

    private lateinit var shareRecyclerView: RecyclerView
    private lateinit var btnDisconnect:Button
    private lateinit var btnSendMore:Button
    private lateinit var extraActionContainer:ConstraintLayout

    private lateinit var service: SocketTransferService

    private lateinit var adapter: ShareRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //recyclerHandler = Handler(Looper.getMainLooper())
        helperItems = ArrayList()

        try {

            itemsToSend = if (savedInstanceState != null){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getSerializable(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                }else{
                    savedInstanceState.getSerializable(ITEMS) as ArrayList<FileItem>
                }
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getSerializable(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
                }else{
                    arguments?.getSerializable(ITEMS) as ArrayList<FileItem>
                }
            }

        }catch (npe:NullPointerException){
            Log.d(TAG,"sharing is receive")
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

        shareRecyclerView = view.findViewById(R.id.share_recycler)
        btnDisconnect = view.findViewById<Button?>(R.id.fragment_transfer_disconnect).apply {

            setOnClickListener {

            }
        }
        btnSendMore = view.findViewById<Button?>(R.id.fragment_transfer_send_more).apply {

            setOnClickListener {

                val selectIntent = Intent(requireActivity(),SelectActivity::class.java)
                selectIntent.action = SelectActivity.ACTION_SELECT
                startActivityForResult(selectIntent, SELECT_REQUEST_CODE)
            }
        }
        extraActionContainer = view.findViewById(R.id.con)

        val serviceIntent = Intent(requireActivity(), SocketTransferService::class.java)

        if (!wifiP2pInfo.isGroupOwner)serviceIntent.putExtra(SocketTransferService.EXTRA_HOST,wifiP2pInfo.groupOwnerAddress.hostAddress)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent)
        }else{
            requireActivity().startService(serviceIntent)
        }

        Toast.makeText(requireActivity(),if (wifiP2pInfo.isGroupOwner)"owner" else "no owner",Toast.LENGTH_SHORT).show()

        requireActivity().bindService(serviceIntent,object : ServiceConnection {

            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                val binder = p1 as SocketTransferService.InnerBinder
                val service = binder.getService()
                this@TransferFragment.service = service

                Log.d("SocketService","service bound")

                initShareRecyclerWithService(service)
                setServiceCallbacks(service)

                service.startSession()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {

            }
        },Context.BIND_AUTO_CREATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            itemsToSend = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                data?.getSerializableExtra(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
            }else{
                data?.getSerializableExtra(ITEMS)!! as ArrayList<FileItem>
            }

            service.sendReceiveCommand(itemsToSend!!.toList())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        itemsToSend?.also {
            outState.putSerializable(ITEMS,itemsToSend)
        }
        outState.putParcelable(P2P_INFO,wifiP2pInfo)
    }

    override fun hasCleared(): Boolean {
        return true
    }

    private fun initShareRecyclerWithService(service: SocketTransferService){

        adapter = ShareRecyclerViewAdapter(helperItems,service)

        shareRecyclerView.adapter = adapter
        shareRecyclerView.layoutManager = NonPredictiveLinearLayoutManager(requireActivity())

        handler = Handler(Looper.getMainLooper())

    }

    private fun setServiceCallbacks(service: SocketTransferService){

        val socketListener = object :SocketListener{

            override fun onSocket() {
                handler.post {
                    if (itemsToSend != null)service.send(itemsToSend!!)else service.receive()
                }
            }
        }

        val sessionImpl = object :Session.SessionImpl{

            override fun onStarted() {
                handler.post {
                    extraActionContainer.visibility = View.GONE
                }
            }

            override fun onEnded() {
                handler.post {
                    extraActionContainer.visibility = View.VISIBLE
                }
            }
        }

        val transferImpl = object :TransferImpl{

            override fun onSendFiles(startPosition: Int, count: Int, files: List<FileItem>) {
                for (i in 0 until count){

                    helperItems.add(HelperItem(HelperItem.ItemState.ENQUEUED,HelperItem.SENT))

                }

                handler.post {

                    adapter.notifyItemRangeInserted(startPosition,count)
                }
            }

            override fun onStartSend(index: Int, name: String, absPath: String,mime:String?, length: Long) {

                val helper = HelperItem(name,absPath,mime,length,HelperItem.ItemState.STARTED,HelperItem.SENT)
                helperItems.set(index,helper)

                handler.post {

                    adapter.notifyItemChanged(index)
                }

            }

            override fun onBytesSent(index: Int, bytes: Long) {

                Log.d(TAG,"$TAG onBytes")
                helperItems[index].also {
                    it.currentValue+=bytes
                    it.itemState = HelperItem.ItemState.IN_PROGRESS
                }

            }

            override fun onEndSend(index: Int) {

                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.ENDED
                }

                handler.post {

                    adapter.notifyItemChanged(index)
                }
            }

            override fun onReceiveFiles(startPosition: Int, count: Int) {
                for (i in 0 until count){

                    helperItems.add(HelperItem(HelperItem.ItemState.ENQUEUED,HelperItem.RECEIVED))
                }

                handler.post {

                    adapter.notifyItemRangeInserted(startPosition,count)
                }
            }

            override fun onStartReceive(index: Int, name: String, absPath: String,mime:String?, length: Long) {

                val helper = HelperItem(name,absPath,mime,length,HelperItem.ItemState.STARTED,HelperItem.RECEIVED)
                helperItems[index] = helper

                handler.post {

                    adapter.notifyItemChanged(index)
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

                handler.post {

                    adapter.notifyItemChanged(index)
                }
            }

            override fun onSkipped(index: Int) {
                helperItems[index].also {
                    it.itemState = HelperItem.ItemState.SKIPPED
                    //adapter.notifyItemChanged(index)
                }

            }

        }

        service.setSocketListener(socketListener)
        service.setTransferImpl(transferImpl)
        service.setSessionImpl(sessionImpl)
    }
}