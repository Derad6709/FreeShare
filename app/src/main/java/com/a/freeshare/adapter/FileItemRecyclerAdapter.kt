package com.a.freeshare.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.viewholder.AbsBaseHolder
import com.a.freeshare.adapter.viewholder.BaseHolder
import com.a.freeshare.obj.FileItem

@SuppressWarnings("notifyDataSetChanged")
class FileItemRecyclerAdapter(items: ArrayList<FileItem>, private var mViewType: Int) :
    SelectableRecyclerViewAdapter<FileItem, BaseHolder>(items) {

    companion object{
        const val LAYOUT_TYPE_LINEAR = 0
        const val LAYOUT_TYPE_GRID = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        val inflater = LayoutInflater.from(parent.context)
        val inflatedView = if (viewType == LAYOUT_TYPE_LINEAR){
            inflater.inflate(R.layout.layout_linear_file_item,parent,false)

        }else {
            inflater.inflate(R.layout.layout_grid_file_item,parent,false)

        }

        return BaseHolder(inflatedView,this)
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {

            holder.bind(items[position])

            holder.itemView.setOnClickListener {

                toggleSelection(items[position])
            }
    }

    override fun getItemViewType(position: Int): Int {
        return mViewType
    }

}