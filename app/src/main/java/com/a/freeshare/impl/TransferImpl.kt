package com.a.freeshare.impl

interface TransferImpl<K> {

    fun onStartSend(o:K)
    fun onBytesSent(o:K,bytes:Long)
    fun onEndSend(o:K)

    fun onStartReceive(o:K)
    fun onBytesReceived(o:K)
    fun onEndReceive(o:K)
}