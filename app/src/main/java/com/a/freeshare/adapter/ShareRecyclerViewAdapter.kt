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
    class ShareRecyclerViewAdapter(private var items:ArrayList<HelperItem>):RecyclerView.Adapter<ShareRecyclerViewAdapter.BaseViewHolder>(){

        open class BaseViewHolder(private val itemView:View):AbsBaseHolder<HelperItem>(itemView){

            private val txtName:TextView = itemView.findViewById(R.id.layout_receive_title)
            private val txtSize:TextView = itemView.findViewById(R.id.layout_receive_size)
            private val imgIcon:ImageView = itemView.findViewById(R.id.layout_receive_icon)

            private lateinit var updateRunnable: Runnable

            override fun bind(a: HelperItem) {

                txtName.text = a.name
                txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"

                updateRunnable = Runnable {

                    if (a.itemState == HelperItem.ItemState.ENDED){
                        txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"
                        setIconOfFile(imgIcon,a.absPath,a.mime)
                        itemView.removeCallbacks(updateRunnable)
                    }else{
                        txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"
                        itemView.postDelayed(updateRunnable,1000)
                    }
                }

                if (a.sharedType == HelperItem.SENT)setIconOfFile(imgIcon,a.absPath,a.mime)
                itemView.postDelayed(updateRunnable,1000)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

            val layoutInflater = LayoutInflater.from(parent.context)

            return if (viewType == TRANSFER_VIEW_TYPE_SEND){
                BaseViewHolder(layoutInflater.inflate(R.layout.layout_send_progress,parent,false))
            }else{
                BaseViewHolder(layoutInflater.inflate(R.layout.layout_receive_progress,parent,false))
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].sharedType
        }

        fun addAndTrack(){

        }
    }
