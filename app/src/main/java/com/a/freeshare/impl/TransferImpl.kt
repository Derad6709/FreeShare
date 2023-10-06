package com.a.freeshare.impl

interface TransferImpl {

    fun onStartSend(index:Int,name:String,absPath:String,mime:String?,length:Long)
    fun onBytesSent(index:Int,bytes:Long)
    fun onEndSend(index: Int)

    fun onStartReceive(index: Int,name: String,absPath: String,mime: String?,length: Long)
    fun onBytesReceived(index: Int,bytes: Long)
    fun onEndReceive(index: Int)
}