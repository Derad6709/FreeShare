package com.a.freeshare.adapter

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.a.freeshare.R
import com.a.freeshare.adapter.viewholder.BaseHolder
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.FileItem
import java.io.File

@SuppressWarnings("notifyDataSetChanged")
class FileItemRecyclerAdapter(items: ArrayList<FileItem>, var mViewType: Int = LAYOUT_TYPE_LINEAR) :
    SelectableRecyclerViewAdapter<FileItem, BaseHolder>(items) {

    companion object{
        const val LAYOUT_TYPE_LINEAR = 0
        const val LAYOUT_TYPE_GRID = 1
    }

    init {
        setSingleSelection(false)
    }

    var listener:OnItemClickListener? = null

    private var gridViewCache:View? = null

    private var animator:ValueAnimator? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        val inflater = LayoutInflater.from(parent.context)

            val view = if (viewType == LAYOUT_TYPE_LINEAR){
                inflater.inflate(R.layout.layout_linear_file_item,parent,false)

            }else {

                    inflater.inflate(R.layout.layout_grid_file_item,parent,false)
            }


        return BaseHolder(view,this,items,listener)
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        super.onBindViewHolder(holder, position)
           holder.bind(items[position])
    }

    override fun onUpdateView(vh: BaseHolder, position: Int) {
        vh.rebind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return mViewType
    }

    override fun isSelectable(position: Int): Boolean {
        return !File(items[position].absPath).isDirectory
    }

}