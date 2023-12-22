package com.a.freeshare

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
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
        const val PORT = 5033
        val TAG = Session::class.simpleName
        private const val NULL_MIME_STRING = "NULL"
        private const val CMD_RCV = "receive_command"
        private const val LOOP = "loop"
        private const val CLOSE = "close"

    }

    private var shouldClose: Boolean = false
    private lateinit var socket:Socket
    private  var serverSocket:ServerSocket? = null

    private lateinit var dataIn:DataInputStream
    private lateinit var dataOut:DataOutputStream

    private lateinit var transferListener:TransferImpl
    private lateinit var socketListener:SocketListener
    private lateinit var sessionImpl:SessionImpl

    private lateinit var newItems:List<FileItem>

    private var context:Context

    private val itemKeep = ArrayList<Any>()

    var sessionState:SessionState = SessionState.IDLE

    private val cancelByFileNames = ArrayList<String>()
    private lateinit var host: String

    private var sessionRunnable: Runnable

    private var cancelWait:Boolean = false

    inner class SendRunnable<T>(private val arg:T) :Runnable{

        override fun run() {
            val fileItems = arg as List<FileItem>

            Log.i(TAG,"files items = ${fileItems.size}")
            //create data i/o from the socket streams

            //first send the total files to be sent
            dataOut.writeInt(fileItems.size)
            dataOut.flush()

            val startIndex = itemKeep.size
            val endIndex = itemKeep.size+fileItems.size

            sessionState = SessionState.STARTED
            sessionImpl.onStarted()

            Log.i(TAG,"send session started")

            for (j in fileItems.indices){
                dataOut.writeUTF(fileItems[j].name)
                dataOut.flush()
            }

            Log.i(TAG,"file names sent")

            transferListener.onSendFiles(startIndex,fileItems.size,fileItems)

            //next iterate through the file items and send files
            for (i in fileItems.indices){

                /*check if there is cancelIndex matching i from other point
                 *if true then skip this loop
                 */
                val cancelByFileName:String = dataIn.readUTF()

                if (cancelByFileName.equals(fileItems[i].name)){

                    transferListener.onSkipped(itemKeep.size)
                    cancelByFileNames.remove(fileItems[i].name)
                    Log.d(TAG,"cancelled from other point @$i")
                    synchronized(itemKeep){
                        itemKeep.add(i)
                    }
                    continue
                }

                /*check if there is cancelIndex matching i from user
                *if true skip else send -1 as cancelIndex to other point
                * */
                if (cancelByFileNames.contains(fileItems[i].name)){

                    transferListener.onSkipped(itemKeep.size)
                    cancelByFileNames.remove(fileItems[i].name)
                    Log.d(TAG,"cancelled by user @$i")

                    synchronized(itemKeep){
                        itemKeep.add(i)
                    }

                    dataOut.writeUTF(fileItems[i].name)
                    dataOut.flush()
                    continue
                }else{
                    dataOut.writeUTF("")
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

                Log.i(TAG,"sending target @ $i")

                val buffer = ByteArray(1024*4)

                Log.d(TAG,"Session onStartSend()")
                transferListener.onStartSend(itemKeep.size,file.name,file.absPath,file.mime,file.dataSize)

                Log.i(TAG,"opening target file input @ $i")

                val inputS:InputStream = if (file.absPath == "N/A"){
                    context.contentResolver.openInputStream(file.content!!)!!
                }else{
                    FileInputStream(file.absPath)
                }

                inputS.use {it:InputStream->

                    while (true){
                        val readLen = it.read(buffer)
                        if (readLen == -1)break

                        dataOut.write(buffer,0,readLen)
                        dataOut.flush()

                        Log.d(TAG,"session onBytes")
                        transferListener.onBytesSent(itemKeep.size, readLen.toLong())
                    }
                }

                inputS.close()

                Log.i(TAG,"target sent at @ $i")

                transferListener.onEndSend(itemKeep.size)

                synchronized(itemKeep){
                    itemKeep.add(i)
                }
            }

            sessionState = SessionState.ENDED
            sessionImpl.onEnded()

            Log.i(TAG,"session ended due to reason : targets finished")


            Log.i(TAG,"waiting ... for next send or receive command")
            try {

                while (true){

                    val cmd = dataIn.readUTF()

                    if (cmd.equals(CMD_RCV)){
                        cancelWait = false
                        receiveRunnable.run()
                        break
                    }else if (cmd.equals(CLOSE)){

                        closeSockets()
                        socketListener.onSocketClosed()
                        break
                    }

                    if (cancelWait){

                        cancelWait = false
                        dataOut.writeUTF(CMD_RCV)
                        dataOut.flush()

                        SendRunnable(newItems).run()
                        break
                    }else{

                        dataOut.writeUTF(if (!shouldClose) LOOP else CLOSE)
                        dataOut.flush()

                        if (shouldClose){

                            closeSockets()

                            socketListener.onSocketClosed()
                            break
                        }
                    }
                }
            }catch (eof:EOFException){
                eof.printStackTrace()
            }catch (se:SocketException){
                se.printStackTrace()
            }
        }
    }

    private lateinit var receiveRunnable:Runnable

    init {
        receiveRunnable = Runnable{

            //get total items count to be received
            Log.i("Session ","receiving total count")
            val itemCount = dataIn.readInt()
            Log.i("Session","$itemCount items receiving")

            val startIndex = itemKeep.size
            val endIndex = itemKeep.size+itemCount

            sessionState = SessionState.STARTED
            sessionImpl.onStarted()

            var queuedNames: Array<String?> = arrayOfNulls(itemCount)

            for (k in 0 until itemCount){
                val nextName = dataIn.readUTF()
                queuedNames[k]= nextName
            }

            Log.i(TAG,"reading file names with count")

            transferListener.onReceiveFiles(startIndex, itemCount,queuedNames)

            Log.i(TAG,"iterating through items")
            //next iterate through the file items and send files
            for (i in 0 until itemCount){

                //check if user cancelIndex == i
                if (cancelByFileNames.contains(queuedNames[i])){

                    transferListener.onSkipped(itemKeep.size)
                    cancelByFileNames.remove(queuedNames[i])

                    synchronized(itemKeep){
                        itemKeep.add(i)
                    }

                    dataOut.writeUTF(queuedNames[i])
                    dataOut.flush()
                    continue
                }else{
                    dataOut.writeUTF("")
                    dataOut.flush()
                }

                /*check if there is cancelIndex matching i from other point
                 *if true then skip this loop
                 */
                val cancelByFileName:String = dataIn.readUTF()

                if (cancelByFileName.equals(queuedNames[i])){
                    transferListener.onSkipped(itemKeep.size)
                    cancelByFileNames.remove(queuedNames[i])
                    synchronized(itemKeep){
                        itemKeep.add(i)
                    }
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

                transferListener.onStartReceive(itemKeep.size,fileName,outFile.absolutePath,mime,fileLength)

                Log.i(TAG,"opening file output for target @ $i")

                FileOutputStream(outFile).use {

                    while (fileLength >0){
                        val receiveSize = if (fileLength > buffer.size) buffer.size else fileLength
                        val receivedBytes = dataIn.read(buffer,0, receiveSize.toInt())

                        fileLength-=receivedBytes

                        it.write(buffer,0,receivedBytes)

                        transferListener.onBytesReceived(itemKeep.size,receivedBytes.toLong())
                        Log.d("Session","onBytesReceived")
                    }
                }

                Log.i(TAG,"file target @ $i closed reason: file ended")

                transferListener.onEndReceive(itemKeep.size)

                synchronized(itemKeep){
                    itemKeep.add(i)
                }

            }

            sessionState = SessionState.ENDED
            sessionImpl.onEnded()

            Log.i(TAG,"waiting for next send or receive")

            try {

                while (true){

                    if (cancelWait){

                        cancelWait = false
                        dataOut.writeUTF(CMD_RCV)
                        dataOut.flush()

                        SendRunnable(newItems).run()
                        break
                    }else{

                        dataOut.writeUTF(if (!shouldClose) LOOP else CLOSE)
                        dataOut.flush()

                        if (shouldClose){

                            closeSockets()
                            socketListener.onSocketClosed()
                            break
                        }
                    }

                    val cmd = dataIn.readUTF()

                    if (cmd.equals(CMD_RCV)){
                        cancelWait = false
                        receiveRunnable.run()
                        break
                    }else if (cmd.equals(CLOSE)){

                        closeSockets()
                        socketListener.onSocketClosed()
                        break
                    }
                }
            }catch (eof:Exception){
                eof.printStackTrace()
            }catch (se:SocketException){
                se.printStackTrace()
            }
        }

    }

    constructor(context:Context, @NonNull socketListener: SocketListener, @NonNull transferListener: TransferImpl,@NonNull sessionImpl: SessionImpl){

        this.transferListener = transferListener
        this.socketListener = socketListener
        this.sessionImpl = sessionImpl
        this.context = context

        sessionRunnable = Runnable {

            Log.i(TAG,"server starting to accept clients")

            sessionState = SessionState.STARTING
            serverSocket = ServerSocket(PORT)

            serverSocket?.reuseAddress = true

            socket = serverSocket!!.accept()

            Log.i(TAG,"server connected to client")

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

            Log.i(TAG,"client trying to connect at port $PORT @ $host")

            sessionState = SessionState.STARTING

            while (true){

                try {

                    socket = Socket(host, PORT)

                    Log.i(TAG,"client connected to server")

                    dataIn = DataInputStream(socket.getInputStream())
                    dataOut = DataOutputStream(socket.getOutputStream())

                    socketListener.onSocket()

                    break
                }catch (se:SocketException){
                    Log.i(TAG,"retrying connect")
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
    fun addToCancelIndex(cancelByName:String){
        cancelByFileNames.add(cancelByName)
    }

    fun send(fileItems:List<FileItem>){

        if (sessionState == SessionState.STARTING){
            Executors.newSingleThreadExecutor().execute(SendRunnable(fileItems))
        }else if (sessionState == SessionState.ENDED){
            sendReceiveCommand(fileItems)
        }
    }

    fun sendReceiveCommand(fileItems: List<FileItem>){

        this.newItems = fileItems
        cancelWait = true
    }

    fun receive(){

        Executors.newSingleThreadExecutor().execute(receiveRunnable)
    }


    private fun createOutDirPerMimeType(@NonNull mime:String):String{

        val externalStorageDirPath  = Environment.getExternalStorageDirectory().absolutePath
        val stringBuilder = StringBuilder().append(externalStorageDirPath).append(File.separator).append(context.getString(R.string.app_name)).append(File.separator)


                if(mime.startsWith("image/")){
                   stringBuilder.append("image")
                }else if(mime.startsWith("video/")){
                    stringBuilder.append("video")
                }else if(mime.startsWith("audio/")){
                    stringBuilder.append("audio")
                }else if(mime.equals("application/vnd.android.package-archive")){
                    stringBuilder.append("app")
                }else{
                    stringBuilder.append("other")
                }
            

        return stringBuilder.toString()

    }

    fun close(){

        shouldClose = true

    }
    private fun closeSockets(){
        synchronized(socket){
            socket.close()
        }

        serverSocket?.also {
            synchronized(it){
                it.close()
            }
        }
    }
}
