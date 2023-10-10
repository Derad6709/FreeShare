package com.a.freeshare.impl

import com.a.freeshare.obj.FileItem
import java.text.FieldPosition

interface TransferImpl {

    fun onStartSend(index:Int,name:String,absPath:String,mime:String?,length:Long)

    fun onBytesSent(index:Int,bytes:Long)
    fun onEndSend(index: Int)

    fun onStartReceive(index: Int,name: String,absPath: String,mime: String?,length: Long)
    fun onBytesReceived(index: Int,bytes: Long)
    fun onEndReceive(index: Int)

    fun onSkipped(index: Int)
    fun onSendFiles(startPosition:Int,count:Int,files:List<FileItem>)
    fun onReceiveFiles(startPosition:Int,count:Int)
}