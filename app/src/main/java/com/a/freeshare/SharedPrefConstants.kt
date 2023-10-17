package com.a.freeshare

import android.content.Context

class SharedPrefConstants(private val context: Context) {

    companion object{
        const val SORT_ORDER = "sort"
        const val SORT_ORDER_FILE = "sort_file"
        const val SORT_ORDER_IMAGE = "sort_image"
        const val SORT_ORDER_APP = "sort_app"
        const val SORT_ORDER_VIDEO = "sort_video"
        const val SORT_ORDER_AUDIO = "sort_audio"
        const val SETTINGS_PREF = "settings"
    }

}