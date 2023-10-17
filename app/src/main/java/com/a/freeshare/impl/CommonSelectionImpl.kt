package com.a.freeshare.impl

import androidx.annotation.IdRes

interface CommonSelectionImpl<K>{
    fun sort(@IdRes sortType:Int)
    fun toggleGridLayout()
    fun isGridLayout():Boolean
    fun getTotalItems():List<K>
    fun getSelectedItems():List<K>
    fun getSelectionHashMap():HashMap<Long,Int>
    fun clearSelection()
    fun selectInverse()
    fun selectAll()
}