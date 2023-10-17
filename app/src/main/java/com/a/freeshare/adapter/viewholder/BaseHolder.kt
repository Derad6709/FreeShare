package com.a.freeshare.adapter.viewholder

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.a.freeshare.R
import com.a.freeshare.adapter.SelectableRecyclerViewAdapter
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.FileUtil

open class BaseHolder(itemView:View,private val selectableAdapter:SelectableRecyclerViewAdapter<FileItem,BaseHolder>,private val items:ArrayList<FileItem>,onClickListener: View.OnClickListener?) : AbsBaseHolder<FileItem>(itemView,onClickListener) {

    private val icon: ImageView = itemView.findViewById<ImageView?>(R.id.layout_file_item_icon)
    private val txtTitle:TextView = itemView.findViewById(R.id.layout_file_item_title)
    private val txtSizeAndDur: TextView = itemView.findViewById(R.id.layout_file_item_size_and_duration)

    private val selectPanel:FrameLayout = itemView.findViewById(R.id.layout_file_item_select_panel)
    private val selectPanelCheck:ImageView = itemView.findViewById(R.id.layout_file_item_select_panel_check)

    override fun bind(a: FileItem) {

        if (selectableAdapter.isSelected(a.hashCode().toLong())){
            selectPanel.visibility = View.VISIBLE
            selectPanelCheck.visibility = View.VISIBLE

            //icon.alpha = 0.0f
        } else{
            selectPanel.visibility = View.GONE
            selectPanelCheck.visibility = View.GONE
            //icon.alpha = 1f

        }

        setIconOfFile(items.indexOf(a),icon,a.absPath,a.mime)

        txtTitle.text = a.name
        txtSizeAndDur.text = FileUtil.getFormattedLongData(a.dataSize)

    }

}