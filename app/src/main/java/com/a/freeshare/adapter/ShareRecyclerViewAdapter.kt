package com.a.freeshare.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.SocketTransferService
import com.a.freeshare.adapter.viewholder.AbsBaseHolder
import com.a.freeshare.fragment.connection.TransferFragment
import com.a.freeshare.obj.HelperItem
import com.a.freeshare.util.FileUtil

class ShareRecyclerViewAdapter(private var items:ArrayList<HelperItem>, private val service: SocketTransferService): RecyclerView.Adapter<ShareRecyclerViewAdapter.BaseViewHolder>(){

    inner class BaseViewHolder(private val itemView: View): AbsBaseHolder<HelperItem>(itemView){

        private val txtName: TextView = itemView.findViewById(R.id.layout_progress_title)
        private val txtSize: TextView = itemView.findViewById(R.id.layout_progress_size)
        private val imgIcon: ImageView = itemView.findViewById(R.id.layout_progress_icon)
        private val cancelIcon: ImageView = itemView.findViewById(R.id.layout_progress_cancel)

        override fun bind(a: HelperItem) {
            setItemState(a)
        }

        private fun setItemState(a:HelperItem){

            if (a.itemState == HelperItem.ItemState.ENQUEUED || a.itemState == HelperItem.ItemState.STARTED || a.itemState == HelperItem.ItemState.IN_PROGRESS){

                if (a.itemState == HelperItem.ItemState.ENQUEUED ){
                    cancelIcon.setOnClickListener {
                        service.addToCancelIndex(items.indexOf(a))
                    }
                }else if (a.itemState == HelperItem.ItemState.STARTED){

                    if (a.sharedType == HelperItem.SENT){
                        setIconOfFile(imgIcon,a.absPath!!,a.mime)}


                    val sizeText = FileUtil.getFormattedLongData(a.currentValue).plus("/").plus(FileUtil.getFormattedLongData(a.maxValue!!))

                    txtName.text = a.name
                    txtSize.text = sizeText
                    cancelIcon.visibility = View.GONE
                }else{

                    val sizeText = FileUtil.getFormattedLongData(a.currentValue).plus("/").plus(FileUtil.getFormattedLongData(a.maxValue!!))

                    txtName.text = a.name
                    txtSize.text = sizeText
                    cancelIcon.visibility = View.GONE

                    Thread{

                        while (a.itemState != HelperItem.ItemState.ENDED){

                            val sizeText = FileUtil.getFormattedLongData(a.currentValue).plus("/").plus(FileUtil.getFormattedLongData(a.maxValue!!))

                            itemView.post {
                                txtSize.text = sizeText
                            }

                            Thread.sleep(1000)
                        }
                    }.start()

                    if (a.sharedType == HelperItem.SENT){
                        setIconOfFile(imgIcon,a.absPath!!,a.mime)}


                }

            }else if(a.itemState == HelperItem.ItemState.ENDED){

                setIconOfFile(imgIcon,a.absPath!!,a.mime)

                val sizeText = FileUtil.getFormattedLongData(a.currentValue).plus("/").plus(FileUtil.getFormattedLongData(a.maxValue!!))

                cancelIcon.setImageDrawable(itemView.context.getDrawable(R.drawable.ic_baseline_check_24))
                cancelIcon.visibility = View.VISIBLE
                txtName.text = a.name
                txtSize.text = sizeText
            }

        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)

        return if (viewType == HelperItem.SENT){
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

}
