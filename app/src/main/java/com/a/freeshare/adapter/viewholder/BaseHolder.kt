package com.a.freeshare.adapter.viewholder

import android.animation.ValueAnimator
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.adapter.SelectableRecyclerViewAdapter
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.FileUtil
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import java.io.File

open class BaseHolder(itemView:View,private val selectableAdapter:SelectableRecyclerViewAdapter<FileItem,BaseHolder>,private val items:ArrayList<FileItem>,onItemClickListener: OnItemClickListener?) : AbsBaseHolder<FileItem>(itemView,onItemClickListener) {

     val icon: ImageView = itemView.findViewById(R.id.layout_file_item_icon)
     val txtTitle:TextView = itemView.findViewById(R.id.layout_file_item_title)
     val txtSizeAndDur: TextView = itemView.findViewById(R.id.layout_file_item_size_and_duration)
     val con = if (selectableAdapter.getItemViewType(adapterPosition) == FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR){
         itemView.findViewById(R.id.con) as ConstraintLayout
     }else{
         itemView.findViewById(R.id.con) as FrameLayout
     }

    private val checkbox:CheckBox = itemView.findViewById(R.id.layout_file_item_check)

    private var color = SurfaceColors.SURFACE_5.getColor(itemView.context)
       private var scaleAnimator:ValueAnimator? = null

    override fun bind(a: FileItem) {

        toggleSelectionUpdate(a)

        setIconOfFile(a.name,items.indexOf(a),icon,a.absPath,a.content,a.mime)

        txtTitle.text = a.name
        txtSizeAndDur.text = FileUtil.getFormattedLongData(a.dataSize)

        if (!selectableAdapter.isSelectable(adapterPosition)){
            checkbox.visibility = View.GONE

        }else{
            checkbox.visibility = View.VISIBLE

        }

    }

    override fun rebind(a: FileItem) {
        toggleSelectionUpdate(a)
    }

    override fun getTextedBitmap(text: String): Bitmap {

        val typedValue = TypedValue()
        itemView.context.theme.resolveAttribute(android.R.attr.colorPrimary,typedValue,true)

        val refBitmap = Bitmap.createBitmap(icon.measuredWidth,icon.measuredHeight,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(refBitmap)
        val paint = Paint()
        paint.color = typedValue.data
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = txtTitle.textSize
        canvas.drawText(text,icon.width.toFloat()/2,((icon.height/2) - ((paint.descent()+paint.ascent())/2)),paint)

        return refBitmap
    }

    private fun toggleSelectionUpdate(a:FileItem){

        con.setBackgroundColor(if (selectableAdapter.isSelected(a.hashCode().toLong())) color else Color.TRANSPARENT)

        checkbox.isChecked = selectableAdapter.isSelected(a.hashCode().toLong())

        if (selectableAdapter.getItemViewType(items.indexOf(a)) == FileItemRecyclerAdapter.LAYOUT_TYPE_GRID){
            itemView.post {

                val margin = checkbox.measuredWidth.toFloat()/2f+(checkbox.layoutParams as FrameLayout.LayoutParams).marginEnd.toFloat()
                val scaledSide:Float = icon.measuredWidth - margin

                val scaleAnimator = ValueAnimator.ofFloat(1f,scaledSide/icon.measuredWidth.toFloat()).apply {
                    duration = 400
                    addUpdateListener {
                        icon.scaleX = it.animatedValue as Float
                        icon.scaleY = it.animatedValue as Float
                    }
                }

                if (selectableAdapter.isSelected(a.hashCode().toLong())){
                    scaleAnimator?.start()
                }else{
                    scaleAnimator?.reverse()
                }
            }
        }
    }
}