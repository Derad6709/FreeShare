package com.a.freeshare.source

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import kotlin.Comparator
import kotlin.collections.ArrayList

class LocalMediaSourceViewModel(private val app:Application) : AndroidViewModel(app) {

    val fileMediaArr = MutableLiveData<ArrayList<FileItem>>()


    companion object{
        private val fileNameMap = URLConnection.getFileNameMap()
        fun getFileItemsFromDirRecursive(sourceDir :String, mimeTypePrefix:String,compare:ArrayList<FileItem>,search:String):ArrayList<FileItem> {

            val mArray = ArrayList<FileItem>()

            val files = File(sourceDir).listFiles()

            files?.also {
                for (f in it){
                    f?.also {
                        if (!f.isDirectory){

                            val mime:String? = fileNameMap.getContentTypeFor(f.name)
                            val fi = FileItem(f, mime)
                            if (mime != null && mime.startsWith(mimeTypePrefix) && !compare.contains(fi) && !mArray.contains(fi)) {
                                mArray.add(fi)
                            }else if (fi.name.contains(search) && !compare.contains(fi) && !mArray.contains(fi)){
                                mArray.add(fi)
                            }
                        }else {

                            mArray.addAll(getFileItemsFromDirRecursive(f.absolutePath,mimeTypePrefix,mArray,search))
                        }
                    }
                }
            }

            return mArray
        }

        fun sort(@IdRes sortType:Int?,sort:Boolean,@Nullable data:MutableLiveData<ArrayList<FileItem>>?):Comparator<FileItem>{

            val sortComparator = object :Comparator<FileItem>{
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
                            return 0
                        }
                    }
                }
            }

            if (sort && data?.value !=null){

                    val sorted = data.value
                    sorted?.sortWith(sortComparator)
                    data.value = sorted

            }

            return sortComparator
        }
    }

    fun loadData(mediaUri:Uri,idColumn:String,dataColumn:String,@Nullable optSearchPath:String?,@Nullable optMimeTypePrefix:String?,@IdRes sortType: Int?){
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

            if (optSearchPath != null && optMimeTypePrefix != null) array.addAll(getFileItemsFromDirRecursive(optSearchPath,optMimeTypePrefix,array,"N/A"))

            if (sortType != null){
                val sorted = ArrayList<FileItem>().apply{
                    addAll(array.sortedWith(sort(sortType,false,null))!!)
                }
                array.clear()
                array.addAll(sorted)
            }

            fileMediaArr.postValue(array)

        }.start()
    }

    fun loadDirectoryContent(absPath:String,@IdRes sortType: Int?){

        Thread{

            val array = ArrayList<FileItem>()
            File(absPath)?.also {

                for (f in it.listFiles()){
                    val mime = fileNameMap.getContentTypeFor(f.name)
                    array.add(FileItem(f,mime))
                }
            }

            if (sortType != null){
                val sorted = ArrayList<FileItem>().apply{
                    addAll(array.sortedWith(sort(sortType,false,null))!!)
                }
                array.clear()
                array.addAll(sorted)
            }

            fileMediaArr.postValue(array)

        }.start()
    }

    fun loadApks(@IdRes sortType:Int?){

        val items = ArrayList<FileItem>()

        val installedAppsInfo = if (Build.VERSION.SDK_INT >= 33) {
            app.packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        }else{
            app.packageManager.getInstalledApplications(0)
        }

        Thread{

            for (a in installedAppsInfo){

                if (a.flags and ApplicationInfo.FLAG_SYSTEM == 0){

                    val name = "${a.loadLabel(app.packageManager)}.apk"
                    val absPath = a.publicSourceDir
                    val apkFile = File(absPath)
                    val dataSize = apkFile.length()
                    val mime = fileNameMap.getContentTypeFor(File(absPath).name)

                    items.add(FileItem(name,absPath,dataSize,apkFile.lastModified(),mime))
                }
            }

            if (sortType != null){
                val sorted = ArrayList<FileItem>().apply{
                    addAll(items.sortedWith(sort(sortType,false,null))!!)
                }
                items.clear()
                items.addAll(sorted)
            }

            fileMediaArr.postValue(items)

        }.start()
    }

    fun fromSource(items:ArrayList<FileItem>){
        fileMediaArr.value = items
    }


}