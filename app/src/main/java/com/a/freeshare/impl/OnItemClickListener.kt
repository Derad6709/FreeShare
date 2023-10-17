package com.a.freeshare.impl

import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface OnItemClickListener {

    fun onItemClick(v:View?,itemPosition:Int,vh:RecyclerView.ViewHolder)
}