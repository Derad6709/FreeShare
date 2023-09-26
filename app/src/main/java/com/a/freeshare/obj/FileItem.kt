package com.a.freeshare.obj

import android.os.FileUtils
import androidx.annotation.Nullable
import com.a.freeshare.util.FileUtil
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import java.io.Serializable
import java.net.URLConnection

class FileItem :Serializable{

     var name:String
     var absPath:String
     var dataSize:Long
     var lastMod:Long
     @Nullable var mime:String?

    constructor(name:String, absPath:String, dataSize:Long,lastMod:Long, @Nullable mime:String?) {
       this.name = name
       this.absPath  =absPath
       this.dataSize = dataSize
       this.lastMod = lastMod
       this.mime = mime
    }

    constructor(f:File, @Nullable mime: String?){
        this.name = f.name
        this.absPath = f.absolutePath
        this.dataSize = f.length()
        this.lastMod = f.lastModified()
        this.mime = mime
    }

    fun copy():FileItem{
        return FileItem(name, absPath, dataSize, lastMod, mime)
    }

}