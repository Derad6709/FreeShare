package com.a.freeshare.obj

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class HelperItem(var name:String?, var absPath:String?, var content: Uri?, var mime:String?, var maxValue:Long?, var itemState: Int, var sharedType:Int, var currentValue:Long = 0):Parcelable{

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong()
    ) {
    }

    class ItemState{
        companion object{
            const val STARTED = 0x00
             const val IN_PROGRESS = 0x01
            const val ENDED = 0x02
            const val ENQUEUED = 0x03
            const val SKIPPED = 0x04

            const val SENT = 0x05
            const val RECEIVED = 0x06
         }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(absPath)
        parcel.writeParcelable(content, flags)
        parcel.writeString(mime)
        parcel.writeValue(maxValue)
        parcel.writeInt(itemState)
        parcel.writeInt(sharedType)
        parcel.writeLong(currentValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HelperItem> {
        override fun createFromParcel(parcel: Parcel): HelperItem {
            return HelperItem(parcel)
        }

        override fun newArray(size: Int): Array<HelperItem?> {
            return arrayOfNulls(size)
        }


    }

}