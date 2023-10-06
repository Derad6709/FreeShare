package com.a.freeshare.adapter.viewholder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.view.View
import android.widget.ImageView
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.SelectableRecyclerViewAdapter
import com.a.freeshare.obj.FileItem
import com.bumptech.glide.Glide
import java.io.File
import java.net.URLConnection
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class AbsBaseHolder<K>(itemView: View):RecyclerView.ViewHolder(itemView){

    abstract fun bind(a: K)

    protected fun setIconOfFile(icon:ImageView,absPath:String,mimeType:String?){


        val sourceFile = File(absPath)

        if (sourceFile.isDirectory){

            icon.setImageDrawable(ContextCompat.getDrawable(itemView.context,R.drawable.ic_baseline_folder_24))
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }else{

            val mime:String? = mimeType

            if (mime != null){

                if (mime.startsWith("image/")){

                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(itemView.context).load(sourceFile).into(icon)
                }else if (mime.startsWith("video/")){
                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(itemView.context).load(sourceFile.absolutePath).into(icon)


                }else if (mime.startsWith("audio/")){

                    val mmr = MediaMetadataRetriever()
                    synchronized(mmr){
                        mmr.setDataSource(sourceFile.absolutePath)
                    }
                    val byteArray = mmr.embeddedPicture
                    mmr.close()
                    if (byteArray!=null){
                        icon.scaleType = ImageView.ScaleType.CENTER_CROP
                        Glide.with(itemView.context).load(byteArray).into(icon)
                    }else{
                        icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        icon.setImageDrawable(ContextCompat.getDrawable(itemView.context,R.drawable.ic_baseline_music_note_24))
                    }

                }else if (mime.contains("vnd")){

                    val pi = itemView.context.applicationContext.packageManager.getPackageArchiveInfo(sourceFile.absolutePath,0)
                    pi?.applicationInfo?.sourceDir = sourceFile.absolutePath
                    pi?.applicationInfo?.publicSourceDir = sourceFile.absolutePath

                    Thread{

                        val logo = pi?.applicationInfo?.loadIcon(itemView.context.applicationContext.packageManager)

                        icon.post {
                            icon.setImageDrawable(logo)
                            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        }
                    }.start()

                }else{

                    icon.setImageDrawable(null)
                    icon.scaleType = ImageView.ScaleType.FIT_XY
                }
            }else{
                icon.setImageDrawable(null)
                icon.scaleType = ImageView.ScaleType.FIT_XY

            }

        }

    }

    protected fun getScaledBitmap(bmp:Bitmap,percent : Float):Bitmap{

        val scaledWidth:Int = (bmp.width.toFloat() * percent).roundToInt()
        val scaledHeight:Int = (bmp.height.toFloat() * percent).roundToInt()

        return Bitmap.createScaledBitmap(bmp,scaledWidth,scaledHeight,true)
    }
}