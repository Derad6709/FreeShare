package com.a.freeshare.adapter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.SocketTransferService
import com.a.freeshare.adapter.viewholder.AbsBaseHolder
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.HelperItem
import com.a.freeshare.util.FileUtil
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.round

class ShareRecyclerViewAdapter(private var items:ArrayList<HelperItem>, private val service: SocketTransferService?,private var itemClickListener: OnItemClickListener?): RecyclerView.Adapter<ShareRecyclerViewAdapter.BaseViewHolder>(){

    inner class BaseViewHolder(private val itemView: View): AbsBaseHolder<HelperItem>(itemView,null){

        private val txtName: TextView = itemView.findViewById(R.id.layout_progress_title)
        private val txtSize: TextView = itemView.findViewById(R.id.layout_progress_size)
        private val imgIcon: ImageView = itemView.findViewById(R.id.layout_progress_icon)
        private val progressBar:LinearProgressIndicator = itemView.findViewById<LinearProgressIndicator?>(R.id.layout_progress_progress).apply {
            max = 100
        }
        private val btnCancel:Button = itemView.findViewById(R.id.button)

        override fun bind(a: HelperItem) {
            setItemState(a)
        }

        private fun setItemState(a:HelperItem){

            btnCancel.setOnClickListener {

                when(a.itemState){

                    HelperItem.ItemState.ENQUEUED->{
                        service?.addToCancelIndex(a.name!!)
                        btnCancel.isEnabled = false
                        txtName.text = itemView.context.getString(R.string.cancelled)
                        txtSize.text = itemView.context.getString(R.string.cancelled)
                    }

                    HelperItem.ItemState.ENDED->{

                        itemClickListener?.onItemClick(it,adapterPosition,this)
                    }

                    else->{

                    }
                }
            }

            if (a.itemState == HelperItem.ItemState.ENQUEUED){

                if (a.sharedType == HelperItem.ItemState.SENT){
                    setIconOfFile(a.name!!,items.indexOf(a),imgIcon,a.absPath,a.content,a.mime)
                }else{
                    imgIcon.setImageDrawable(null)
                }

                txtName.text = a.name
                txtSize.text = itemView.context.getString(R.string.waiting)
                progressBar.visibility = View.GONE
                btnCancel.apply {
                    isEnabled = true
                    text = itemView.context.getString(R.string.cancel)
                }

            }else if (a.itemState == HelperItem.ItemState.STARTED || a.itemState == HelperItem.ItemState.IN_PROGRESS){

                if (a.sharedType == HelperItem.ItemState.SENT){
                    setIconOfFile(a.name!!,items.indexOf(a),imgIcon,a.absPath,a.content,a.mime)
                }

                txtName.text = a.name
                progressBar.visibility = View.VISIBLE
                btnCancel.apply {
                    isEnabled = true
                    text = itemView.context.getString(R.string.cancel)
                }
                trackData(a)

            }else if (a.itemState == HelperItem.ItemState.ENDED){


                setIconOfFile(a.name!!,items.indexOf(a),imgIcon,a.absPath,a.content,a.mime)

                txtName.text = a.name
                progressBar.visibility = View.GONE
                updateProgressViews(a)

                btnCancel.apply {
                    isEnabled = true
                    text = "Open"
                }

            }else if (a.itemState == HelperItem.ItemState.SKIPPED){
                btnCancel.apply {
                    isEnabled = false
                    text = itemView.context.getString(R.string.cancel)
                }
                txtName.text = itemView.context.getString(R.string.cancelled)
                txtSize.text = itemView.context.getString(R.string.cancelled)
                progressBar.visibility = View.GONE
                imgIcon.setImageDrawable(null)
            }

        }

        private fun trackData(a:HelperItem){

            Thread{
                 val itemIndex = items.indexOf(a)

                while (items[itemIndex].itemState != HelperItem.ItemState.ENDED && !Thread.interrupted()){

                    itemView.post {
                        if (itemIndex == adapterPosition){
                            updateProgressViews(items[itemIndex])
                        }
                    }

                    try {
                        Thread.sleep(1000)
                    }catch (ie:InterruptedException){
                        ie.printStackTrace()
                    }
                }

            }.start()
        }

        override fun getTextedBitmap(text: String): Bitmap {

            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(android.R.attr.colorPrimary,typedValue,true)

            val refBitmap = Bitmap.createBitmap(imgIcon.measuredWidth,imgIcon.measuredHeight,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(refBitmap)
            val paint = Paint()
            paint.color = typedValue.data
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = txtName.textSize
            canvas.drawText(text,imgIcon.width.toFloat()/2,((imgIcon.height/2) - ((paint.descent()+paint.ascent())/2)),paint)

            return refBitmap
        }


        private fun updateProgressViews(a:HelperItem){
            val sizeText = FileUtil.getFormattedLongData(a.currentValue).plus("/").plus(FileUtil.getFormattedLongData(a.maxValue!!))
            txtSize.text = sizeText
            progressBar.progress = if (a.currentValue == 0L) 0 else round((a.currentValue.toFloat()/a.maxValue!!.toFloat())*100).toInt()
        }

        override fun rebind(a: HelperItem) {

        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)

        return if (viewType == HelperItem.ItemState.SENT){
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
