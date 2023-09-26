package com.a.freeshare.impl

interface CommonSelectionImpl<K>{
    fun getTotalItems():List<K>
    fun getSelectedItems():List<K>
    fun getSelection():ArrayList<Long>
}