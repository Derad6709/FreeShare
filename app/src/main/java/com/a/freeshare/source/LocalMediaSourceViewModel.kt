package com.a.freeshare.source

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.a.freeshare.R
import com.a.freeshare.obj.FileItem
import java.io.File
import java.net.URLConnection
import java.util.*
import kotlin.collections.ArrayList

class LocalMediaSourceViewModel(private val app:Application) : AndroidViewModel(app) {

    val fileMediaArr = MutableLiveData<ArrayList<FileItem>>()
    private val fileNameMap = URLConnection.getFileNameMap()

    fun loadData(mediaUri:Uri,idColumn:String,dataColumn:String,@Nullable optSearchPath:String?,@Nullable optMimeTypePrefix:String?){
        val array = arrayListOf<FileItem>()

        val c = app.contentResolver.query(
            mediaUri,
            arrayOf(idColumn, dataColumn,MediaStore.MediaColumns.MIME_TYPE),null,null,null)

        Thread {
            if (c!!.moveToFirst()) {
                while (c.moveToNext()) {
                    val dataIndex = c.getColumnIndexOrThrow(dataColumn)
                    val absPath = c.getString(dataIndex)
                    val mimeIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val mime = c.getString(mimeIndex)

                    array.add(FileItem(File(absPath),mime))
                }
            }

            if (array.size == 0){
                if (optSearchPath != null && optMimeTypePrefix != null) array.addAll(getFileItemsFromDirRecursive(optSearchPath,optMimeTypePrefix))
            }

            fileMediaArr.postValue(array)
        }.start()
    }

    fun sort(@IdRes sortType:Int){

                val sorted = fileMediaArr.value?.sortedWith(object :Comparator<FileItem>{
                    override fun compare(p0: FileItem?, p1: FileItem?): Int {

                        when(sortType){

                            R.id.sort_by_new->{
                                return p1!!.lastMod.compareTo(p0!!.lastMod)
                            }

                            R.id.sort_by_old->{
                                return p0!!.lastMod.compareTo(p1!!.lastMod)
                            }

                            R.id.sort_by_az->{
                                return p0!!.name.get(0).compareTo(p1!!.name.get(0))
                            }

                            R.id.sort_by_za->{
                                return p1!!.name.get(0).compareTo(p0!!.name.get(0))
                            }

                            R.id.sort_by_large->{
                                return p1!!.dataSize.compareTo(p0!!.dataSize)
                            }

                            R.id.sort_by_small->{
                                return p0!!.dataSize.compareTo(p1!!.dataSize)
                            }

                            else->{
                                return -1
                            }
                        }
                    }
                })

                fileMediaArr.value = ArrayList<FileItem>().apply { addAll(sorted!!.toList()) }

                Log.i("LocalMediaSource","sort by a_z")

    }

    private fun getFileItemsFromDirRecursive(sourceDir :String, mimeTypePrefix:String):ArrayList<FileItem> {

        val mArray = ArrayList<FileItem>()

        val files = File(sourceDir).listFiles()

        files?.also {
            for (f in it){
                f?.also {
                    if (!f.isDirectory){
                        val mime:String? = fileNameMap.getContentTypeFor(f.name)
                        if (mime != null && mime.startsWith(mimeTypePrefix))mArray.add(FileItem(f, mime))
                    }else {
                        mArray.addAll(getFileItemsFromDirRecursive(f.absolutePath,mimeTypePrefix))
                    }
                }
            }
        }

        return mArray
    }

}