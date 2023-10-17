package com.a.freeshare.obj

import android.net.Uri
import android.os.FileUtils
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Nullable
import com.a.freeshare.util.FileUtil
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import java.io.Serializable
import java.net.URLConnection

class FileItem() :Parcelable{

     lateinit var name:String
     var absPath:String = "N/A"
     var content:Uri? = Uri.EMPTY
     var dataSize:Long = 0
     var lastMod:Long = 0
     var mime:String? = "N/A"

    constructor(parcel: Parcel) : this() {
        name = parcel.readString().toString()
        absPath = parcel.readString().toString()
        content = parcel.readParcelable(Uri::class.java.classLoader)
        dataSize = parcel.readLong()
        lastMod = parcel.readLong()
        mime = parcel.readString()
    }


    constructor(name:String,@Nullable absPath:String?, dataSize:Long,lastMod:Long, @Nullable mime:String?) : this() {
       this.name = name
       if (absPath != null) this.absPath  = absPath
       this.dataSize = dataSize
       this.lastMod = lastMod
       this.mime = mime
    }

    constructor(name: String,content:Uri,dataSize: Long,lastMod: Long,mime: String?) : this() {
        this.name = name
        this.content = content
        this.dataSize = dataSize
        this.mime = mime
        this.lastMod = lastMod
    }

    constructor(f:File, @Nullable mime: String?) : this() {
        this.name = f.name
        this.absPath = f.absolutePath
        this.dataSize = f.length()
        this.lastMod = f.lastModified()
        this.mime = mime
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(absPath)
        parcel.writeParcelable(content, flags)
        parcel.writeLong(dataSize)
        parcel.writeLong(lastMod)
        parcel.writeString(mime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileItem> {
        override fun createFromParcel(parcel: Parcel): FileItem {
            return FileItem(parcel)
        }

        override fun newArray(size: Int): Array<FileItem?> {
            return arrayOfNulls(size)
        }
    }

    override fun hashCode(): Int {
        return absPath.hashCode()
    }
}