package com.a.freeshare.fragment.connection

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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.SocketTransferService
import com.a.freeshare.adapter.viewholder.AbsBaseHolder
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.obj.HelperItem
import com.a.freeshare.util.FileUtil
import java.io.File
import java.lang.NullPointerException

class TransferFragment:BaseFragment() {

    class NonPredictiveLinearLayoutManager(context:Context):LinearLayoutManager(context){
        override fun supportsPredictiveItemAnimations(): Boolean {
            return false
        }
    }

    companion object{
        val TAG = TransferFragment::class.simpleName

        private const val TRANSFER_VIEW_TYPE_SEND = 0
        private const val TRANSFER_VIEW_TYPE_RECEIVE = 1
    }

    private var itemsToSend:ArrayList<FileItem>? = null

    private lateinit var transferHandler:Handler

    private lateinit var helperItems : ArrayList<HelperItem>

    private lateinit var wifiP2pInfo: WifiP2pInfo

    private lateinit var shareRecyclerView: RecyclerView

    private lateinit var adapter: ShareRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transferHandler = Handler(Looper.getMainLooper())
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

        adapter = ShareRecyclerViewAdapter(helperItems)

        shareRecyclerView.adapter = adapter
        shareRecyclerView.layoutManager = LinearLayoutManager(requireActivity())

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

                Log.d("SocketService","service bound")

                setServiceCallbacks(service)

                service.startSession()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {

            }
        },Context.BIND_AUTO_CREATE)
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
    private fun setServiceCallbacks(service: SocketTransferService){

        val socketListener = object :SocketListener{

            override fun onSocket() {
                if (itemsToSend != null)service.send(itemsToSend!!)else service.receive()
            }
        }

        val transferImpl = object :TransferImpl{

            override fun onStartSend(index: Int, name: String, absPath: String,mime:String?, length: Long) {
                transferHandler.post {
                    val helper = HelperItem(name,absPath,mime,length,HelperItem.ItemState.STARTED,HelperItem.SENT)
                    helperItems.add(helper)
                    adapter.notifyItemInserted(helperItems.size-1)
                }
            }

            override fun onBytesSent(index: Int, bytes: Long) {

                    helperItems[index].apply {
                        currentValue+=bytes
                        itemState = HelperItem.ItemState.IN_PROGRESS
                    }

            }

            override fun onEndSend(index: Int) {

                    helperItems[index].apply {
                        itemState = HelperItem.ItemState.ENDED
                    }
            }

            override fun onStartReceive(index: Int, name: String, absPath: String,mime:String?, length: Long) {
                transferHandler.post {
                    val helper = HelperItem(name,absPath,mime,length,HelperItem.ItemState.STARTED,HelperItem.RECEIVED)
                    helperItems.add(helper)
                    adapter.notifyItemInserted(helperItems.size-1)
                }
            }

            override fun onBytesReceived(index: Int, bytes: Long) {
                    helperItems[index].apply {
                        currentValue+=bytes
                        itemState = HelperItem.ItemState.IN_PROGRESS
                    }
            }

            override fun onEndReceive(index: Int) {

                    helperItems[index].apply {
                        itemState = HelperItem.ItemState.ENDED
                    }
            }
        }

        service.setSocketListener(socketListener)
        service.setTransferImpl(transferImpl)
    }
}
