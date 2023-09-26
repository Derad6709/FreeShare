package com.a.freeshare.adapter.viewholder

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.a.freeshare.R
import com.a.freeshare.adapter.SelectableRecyclerViewAdapter
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.FileUtil
import com.bumptech.glide.Glide
import java.io.File
import kotlin.math.roundToInt

open class BaseHolder(itemView:View,private val selectableAdapter:SelectableRecyclerViewAdapter<FileItem,BaseHolder>) : AbsBaseHolder<FileItem>(itemView) {

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

        setIconOfFile(a)

        txtTitle.text = a.name
        txtSizeAndDur.text = FileUtil.getFormattedLongData(a.dataSize)

    }

  fun setIconOfFile(item:FileItem){


                val sourceFile = File(item.absPath)

                if (sourceFile.isDirectory){

                        icon.setImageDrawable(ContextCompat.getDrawable(itemView.context,R.drawable.ic_baseline_folder_24))
                        icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }else{

                    val mime:String? = item.mime

                    if (mime != null){

                        if (mime.startsWith("image/")){

                            icon.scaleType = ImageView.ScaleType.CENTER_CROP
                            Glide.with(itemView.context).load(sourceFile).into(icon)
                        }else if (mime.startsWith("video/")){
                            icon.scaleType = ImageView.ScaleType.CENTER_CROP
                            Glide.with(itemView.context).load(sourceFile.absolutePath).into(icon)


                        }else if (mime.startsWith("audio/")){

                            val mmr = MediaMetadataRetriever()
                            mmr.setDataSource(sourceFile.absolutePath)
                            val byteArray = mmr.embeddedPicture
                            synchronized(mmr){
                                mmr.close()
                            }
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

    private fun getScaledBitmap(bmp:Bitmap,percent : Float):Bitmap{

        val scaledWidth:Int = (bmp.width.toFloat() * percent).roundToInt()
        val scaledHeight:Int = (bmp.height.toFloat() * percent).roundToInt()

        return Bitmap.createScaledBitmap(bmp,scaledWidth,scaledHeight,true)
    }
}