package com.a.freeshare.adapter.viewholder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.SelectableRecyclerViewAdapter
import java.io.File
import java.net.URLConnection
import kotlin.math.roundToInt

abstract class AbsBaseHolder<K>(itemView: View):RecyclerView.ViewHolder(itemView){

    abstract fun bind(a: K)

}