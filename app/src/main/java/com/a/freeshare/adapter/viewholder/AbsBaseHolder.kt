package com.a.freeshare.adapter.viewholder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.impl.OnItemClickListener
import com.bumptech.glide.Glide
import java.io.File
import kotlin.math.roundToInt

abstract class AbsBaseHolder<K>(itemView: View,itemClickListener: OnItemClickListener?):RecyclerView.ViewHolder(itemView){

    private val directoryDrawable: Drawable = ContextCompat.getDrawable(itemView.context,R.drawable.ic_baseline_folder_24)!!
    private val audioNoteDrawable:Drawable = ContextCompat.getDrawable(itemView.context,R.drawable.ic_baseline_music_note_24)!!

    init {

        itemView.setOnClickListener{

            itemClickListener?.onItemClick(it, adapterPosition,this as BaseHolder)
        }

    }


    class SquareImageView:androidx.appcompat.widget.AppCompatImageView{

        constructor(context: Context):super(context)
        constructor(context: Context,attrs:AttributeSet):super(context, attrs)

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec,heightMeasureSpec)
            setMeasuredDimension(measuredWidth,measuredWidth)
        }
    }

    abstract fun bind(a: K)
    abstract fun rebind(a:K)

    protected fun setIconOfFile(name:String,position:Int, icon:ImageView, absPath:String?, uri: Uri?, mimeType:String?){

        var sourceFile:File? = null

        absPath?.also {
            sourceFile = File(it)
        }

        itemView.findViewById<FrameLayout>(R.id.layout_file_item_icon_container)?.apply {
            background = null
        }

        if ( sourceFile != null && sourceFile!!.isDirectory){

            icon.setImageDrawable(directoryDrawable)
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE

        }else{

            val mime:String? = mimeType

            if (mime != null){

                if (mime.startsWith("image/")){

                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(itemView.context.applicationContext).load(if (absPath == "N/A") uri else sourceFile).into(icon)

                }else if (mime.startsWith("video/")){
                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(itemView.context.applicationContext).load(if (absPath == "N/A") uri else sourceFile).into(icon)

                }else if (mime.startsWith("audio/")){

                    Thread{

                        val mmr = MediaMetadataRetriever()
                        var byteArray:ByteArray? = null
                        synchronized(mmr){
                            try {
                                mmr.setDataSource(sourceFile!!.absolutePath)
                                byteArray = mmr.embeddedPicture
                            }catch (e:Exception){

                                mmr.close()
                            }
                        }

                        if (byteArray!=null && byteArray!!.isNotEmpty()){
                            itemView.post {
                                if (position == adapterPosition){
                                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                                    Glide.with(itemView.context.applicationContext).load(byteArray).into(icon)

                                }
                            }
                        }else{
                            itemView.post {
                               if (position == adapterPosition){
                                   icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                                   icon.setImageDrawable(audioNoteDrawable)

                               }
                            }
                        }
                    }.start()

                    icon.post {
                        icon.tag = "audio"
                    }
                }else if (mime.contains("vnd") || mime.endsWith(".apk",false)){

                    Thread{

                        val pi = itemView.context.applicationContext.packageManager.getPackageArchiveInfo(sourceFile!!.absolutePath,0)
                        pi?.applicationInfo?.sourceDir = sourceFile!!.absolutePath
                        pi?.applicationInfo?.publicSourceDir = sourceFile!!.absolutePath

                        val logo = pi?.applicationInfo?.loadIcon(itemView.context.applicationContext.packageManager)

                        icon.post {
                          if (position == adapterPosition){

                              icon.setImageDrawable(logo)
                              icon.scaleType = ImageView.ScaleType.FIT_XY
                          }
                        }
                    }.start()

                }else{

                   itemView.post {

                       itemView.findViewById<FrameLayout>(R.id.layout_file_item_icon_container)?.apply {
                           setBackgroundResource(R.drawable.file_item_icon_bg)
                       }

                       val splitArgs = name.split(".",)

                       icon.setImageBitmap(getTextedBitmap(splitArgs.get(splitArgs.size-1)))
                       icon.scaleType = ImageView.ScaleType.FIT_XY

                   }
                }
            }else{

                itemView.post {
                    itemView.findViewById<FrameLayout>(R.id.layout_file_item_icon_container)?.apply {
                        setBackgroundResource(R.drawable.file_item_icon_bg)
                    }

                    val splitArgs = name.split(".",)

                    icon.setImageBitmap(getTextedBitmap(splitArgs.get(splitArgs.size-1)))
                    icon.scaleType = ImageView.ScaleType.FIT_XY

                }
            }

        }

    }

    protected fun getScaledBitmap(bmp:Bitmap,percent : Float):Bitmap{

        val scaledWidth:Int = (bmp.width.toFloat() * percent).roundToInt()
        val scaledHeight:Int = (bmp.height.toFloat() * percent).roundToInt()

        return Bitmap.createScaledBitmap(bmp,scaledWidth,scaledHeight,true)
    }

    protected abstract fun getTextedBitmap(text:String):Bitmap

}