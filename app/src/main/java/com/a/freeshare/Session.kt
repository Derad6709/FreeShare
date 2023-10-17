package com.a.freeshare

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import java.io.*
import java.lang.Exception
import java.lang.NullPointerException
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.ArrayList

class Session {

    enum class SessionState{
        STARTING,
        STARTED,
        IDLE,
        ENDED
    }

    interface SessionImpl{

        fun onStarted()
        fun onEnded()
    }

    companion object{
        const val PORT = 8855
        val TAG = Session::class.simpleName
        const val NULL_MIME_STRING = "NULL"
        const val CMD_RCV = "receive_command"
    }

    private lateinit var socket:Socket
    private lateinit var serverSocket:ServerSocket

    private lateinit var dataIn:DataInputStream
    private lateinit var dataOut:DataOutputStream

    private lateinit var transferListener:TransferImpl
    private lateinit var socketListener:SocketListener
    private lateinit var sessionImpl:SessionImpl

    private var context:Context

    private val itemKeep = ArrayList<Int>()

    var sessionState:SessionState = SessionState.IDLE

    private val cancelIndices = ArrayList<Int>()
    private lateinit var host: String

    private var sessionRunnable: Runnable

    inner class SendRunnable<T>(private val arg:T) :Runnable{

        override fun run() {
            val fileItems = arg as List<FileItem>

            Log.d(TAG,"files items = ${fileItems.size}")
            //create data i/o from the socket streams

            //first send the total files to be sent
            dataOut.writeInt(fileItems.size)
            dataOut.flush()

            val startIndex = itemKeep.size
            val endIndex = itemKeep.size+fileItems.size

            sessionState = SessionState.STARTED
            sessionImpl.onStarted()
            
            transferListener.onSendFiles(startIndex,fileItems.size,fileItems)

            //next iterate through the file items and send files
            for (i in fileItems.indices){

                /*check if there is cancelIndex matching i from other point
                 *if true then skip this loop
                 */
                val cancelIndexFromPoint = dataIn.readInt()

                if (cancelIndexFromPoint == i){
                    transferListener.onSkipped(i)
                    Log.d(TAG,"cancelled from other point @$i")
                    continue
                }

                /*check if there is cancelIndex matching i from user
                *if true skip else send -1 as cancelIndex to other point
                * */
                if (cancelIndices.contains(i)){
                    dataOut.writeInt(i)
                    dataOut.flush()
                    transferListener.onSkipped(i)
                    Log.d(TAG,"cancelled by user @$i")
                    continue
                }else{
                    dataOut.writeInt(-1)
                    dataOut.flush()
                }

                //get the file to be sent
                val file = fileItems[i]

                /*
                val shouldProceed = dataIn.readBoolean()

                if (!shouldProceed){
                    Log.d(TAG,"proceed cancelled by other point")
                    continue
                }*/

                dataOut.writeUTF(file.name)
                dataOut.flush()
                dataOut.writeLong(file.dataSize)
                dataOut.flush()

                if (file.mime != null){
                    dataOut.writeUTF(file.mime)
                    dataOut.flush()
                }else{
                    dataOut.writeUTF(NULL_MIME_STRING)
                    dataOut.flush()
                }

                val buffer = ByteArray(1024*4)

                Log.d(TAG,"Session onStartSend()")
                transferListener.onStartSend(itemKeep.size+i,file.name,file.absPath,file.mime,file.dataSize)

                FileInputStream(File(file.absPath)).use {it:FileInputStream->

                    while (true){
                        val readLen = it.read(buffer)
                        if (readLen == -1)break

                        dataOut.write(buffer,0,readLen)
                        dataOut.flush()

                        Log.d(TAG,"session onBytes")
                        transferListener.onBytesSent(itemKeep.size+i, readLen.toLong())
                    }
                }

                transferListener.onEndSend(itemKeep.size+i)
            }

            for (f in fileItems){
                synchronized(itemKeep){
                    itemKeep.add(fileItems.indexOf(f))
                }
            }

            sessionState = SessionState.ENDED
            sessionImpl.onEnded()

            receive()
        }
    }

    private val receiveRunnable:Runnable = Runnable{

        //get total items count to be received
        Log.d("Session ","receiving total count")
        val itemCount = dataIn.readInt()
        Log.d("Session","$itemCount items receiving")

        val startIndex = itemKeep.size
        val endIndex = itemKeep.size+itemCount

        sessionState = SessionState.STARTED
        sessionImpl.onStarted() 
        
        transferListener.onReceiveFiles(startIndex,itemCount)

        Log.d("Session","iterating through items")
        //next iterate through the file items and send files
        for (i in 0 until itemCount){

            //check if user cancelIndex == i
            if (cancelIndices.contains(i)){
                dataOut.writeInt(i)
                transferListener.onSkipped(i)
                continue
            }else{
                dataOut.writeInt(-1)

            }

            dataOut.flush()

            /*check if there is cancelIndex matching i from other point
             *if true then skip this loop
             */
            val cancelIndexFromPoint = dataIn.readInt()

            if (cancelIndexFromPoint == i){
                transferListener.onSkipped(i)
                continue
            }

            //dataOut.writeBoolean(true)

            Log.d("Session","reading meta data")

            val fileName = dataIn.readUTF()
            var fileLength = dataIn.readLong()
            val mime = dataIn.readUTF()

            val outDirPath = createOutDirPerMimeType(mime)
            val outDir = File(outDirPath)
            if (!outDir.exists())outDir.mkdirs()

            val outFile = File(outDir,fileName)

            val buffer = ByteArray(4*1024)

            transferListener.onStartReceive(itemKeep.size+i,fileName,outFile.absolutePath,mime,fileLength)

            FileOutputStream(outFile).use {

                while (fileLength >0){
                    val receiveSize = if (fileLength > buffer.size) buffer.size else fileLength
                    val receivedBytes = dataIn.read(buffer,0, receiveSize.toInt())

                    fileLength-=receivedBytes

                    it.write(buffer,0,receivedBytes)

                    transferListener.onBytesReceived(itemKeep.size+i,receivedBytes.toLong())
                    Log.d("Session","onBytesReceived")
                }
            }

            transferListener.onEndReceive(itemKeep.size+1)
        }

        for (i in 0 until itemCount){
            synchronized(itemKeep){
                itemKeep.add(i)
            }
        }

        sessionState = SessionState.ENDED
        sessionImpl.onEnded()

        receive()
    }

    constructor(context:Context, @NonNull socketListener: SocketListener, @NonNull transferListener: TransferImpl,@NonNull sessionImpl: SessionImpl){

        this.transferListener = transferListener
        this.socketListener = socketListener
        this.sessionImpl = sessionImpl
        this.context = context

        sessionRunnable = Runnable {

            sessionState = SessionState.STARTING
            serverSocket = ServerSocket(PORT)
            serverSocket.reuseAddress = true

            socket = serverSocket.accept()
            dataIn = DataInputStream(socket.getInputStream())
            dataOut = DataOutputStream(socket.getOutputStream())

            socketListener.onSocket()

        }
    }

    constructor(host:String, context: Context, @NonNull socketListener: SocketListener, @NonNull transferListener: TransferImpl,@NonNull sessionImpl: SessionImpl){

        this.transferListener = transferListener
        this.socketListener = socketListener
        this.sessionImpl = sessionImpl
        this.context = context
        this.host = host

        sessionRunnable = Runnable {

            sessionState = SessionState.STARTING

            val socketChannel = SocketChannel.open()

            while (true){

                try {

                    /*socketChannel.connect(InetSocketAddress(host, PORT))
                    socket = socketChannel.socket()*/

                    socket = Socket(host, PORT)

                    dataIn = DataInputStream(socket.getInputStream())
                    dataOut = DataOutputStream(socket.getOutputStream())

                    socketListener.onSocket()

                    break
                }catch (se:SocketException){
                    Log.d(TAG,"retrying connect")
                    Thread.sleep(1000)
                }catch (ie:IOException){
                    break
                }
            }

        }
    }

    fun startSession(){

        Executors.newSingleThreadExecutor().execute(sessionRunnable)
    }
    fun addToCancelIndex(index:Int){
        cancelIndices.add(index)
    }

    fun send(fileItems:List<FileItem>){
        Executors.newSingleThreadExecutor().execute(SendRunnable(fileItems))
    }

    fun sendReceiveCommand(fileItems: List<FileItem>){


    }

    fun receive(){

        Executors.newSingleThreadExecutor().execute(receiveRunnable)
    }


    private fun createOutDirPerMimeType(@NonNull mime:String):String{

        val externalStorageDirPath  =Environment.getExternalStorageDirectory().absolutePath
        val stringBuilder = StringBuilder().append(externalStorageDirPath).append(File.separator).append(context.getString(R.string.app_name)).append(File.separator)

            when{

                mime.startsWith("image/")->{
                   stringBuilder.append("image")
                }

                mime.startsWith("video/")->{
                    stringBuilder.append("video")
                }

                mime.startsWith("audio/")->{
                    stringBuilder.append("audio")
                }

                mime.equals("application/vnd.android.package-archive")->{
                    stringBuilder.append("app")
                }

                else->{
                    stringBuilder.append("other")
                }
            }

        return stringBuilder.toString()

    }

    fun close(){

        socket?.close()
        serverSocket?.close()
    }
}
