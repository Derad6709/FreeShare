package com.a.freeshare

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem
import java.io.*
import java.lang.StringBuilder
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors

class Session {

    enum class SessionState{
        STARTING,
        STARTED,
        IDLE,
        ENDED
    }

    companion object{
        const val PORT = 8855
        val TAG = Session::class.simpleName
        const val NULL_MIME_STRING = "NULL"
    }

    private lateinit var socket:Socket
    private lateinit var serverSocket:ServerSocket

    private var controlThread: Thread

    private var transferListener:TransferImpl
    private var socketListener:SocketListener

    private var context:Context

    var sessionState:SessionState = SessionState.IDLE

    private val cancelIndices = ArrayList<Int>()
    private lateinit var host: String

    constructor(context:Context, @NonNull socketListener: SocketListener, @NonNull transferListener: TransferImpl){
       this.transferListener = transferListener
        this.socketListener = socketListener
        this.context = context

        controlThread = Thread{
            sessionState = SessionState.STARTING
            serverSocket = ServerSocket(PORT)
            serverSocket.reuseAddress = true

            socket = serverSocket.accept()
            sessionState = SessionState.STARTED
            socketListener?.onSocket()
        }

    }

    constructor(host:String, context: Context, @NonNull socketListener: SocketListener, @NonNull transferListener: TransferImpl){

        this.transferListener = transferListener
        this.socketListener = socketListener
        this.context = context
        this.host = host

        controlThread = Thread{
            sessionState = SessionState.STARTING
            while (true){

                try {
                    socket = Socket(host, PORT)
                    sessionState = SessionState.STARTED
                    socketListener?.onSocket()
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
        controlThread.start()
    }
    fun addToCancelIndex(index:Int){
        cancelIndices.add(index)
    }

    fun getControlThread():Thread = controlThread

    fun send(fileItems:List<FileItem>){

        //create data i/o from the socket streams
        val dataIn = DataInputStream(socket.getInputStream())
        val dataOut = DataOutputStream(socket.getOutputStream())

        //first send the total files to be sent
        dataOut.writeInt(fileItems.size)

        //next iterate through the file items and send files
        for (i in fileItems.indices){

            /*check if there is cancelIndex matching i from other point
             *if true then skip this loop
             */
            val cancelIndexFromPoint = dataIn.readInt()

            if (cancelIndexFromPoint == i){
                Log.d(TAG,"cancelled from other point @$i")
                continue
            }

            /*check if there is cancelIndex matching i from user
            *if true skip else send -1 as cancelIndex to other point
            * */
            if (cancelIndices.contains(i)){
                dataOut.writeInt(i)
                Log.d(TAG,"cancelled by user @$i")
                continue
            }else{
                dataOut.writeInt(-1)
            }

            //get the file to be sent
            val file = fileItems[i]

            val shouldProceed = dataIn.readBoolean()

            if (!shouldProceed){
                Log.d(TAG,"proceed cancelled by other point")
                continue
            }

            dataOut.writeUTF(file.name)
            dataOut.writeLong(file.dataSize)

            if (file.mime != null){
                dataOut.writeUTF(file.mime)
            }else{
                dataOut.writeUTF(NULL_MIME_STRING)
            }

            val buffer = ByteArray(1024*4)

            transferListener?.onStartSend(i,file.name,file.absPath,fileItems[i].mime,file.dataSize)

            FileInputStream(File(file.absPath)).use {

                while (true){
                    val readLen = it.read(buffer)
                    if (readLen == -1)break

                    dataOut.write(buffer,0,readLen)
                    dataOut.flush()

                    transferListener?.onBytesSent(i, readLen.toLong())
                }
            }

            transferListener?.onEndSend(i)
        }

        dataIn.close()
        dataOut.close()
        sessionState = SessionState.ENDED
    }

    fun receive(){

        //create data i/o from the socket streams
        val dataIn = DataInputStream(socket.getInputStream())
        val dataOut = DataOutputStream(socket.getOutputStream())

        //get total items count to be received
        val itemCount = dataIn.readInt()

        for (i in 0 until itemCount){

            //check if user cancelIndex == i
            if (cancelIndices.contains(i)){
                dataOut.writeInt(i)
                continue
            }else{
                dataOut.writeInt(-1)
            }

            /*check if there is cancelIndex matching i from other point
             *if true then skip this loop
             */
            val cancelIndexFromPoint = dataIn.readInt()

            if (cancelIndexFromPoint == i){
                continue
            }

            dataOut.writeBoolean(true)

            val fileName = dataIn.readUTF()
            var fileLength = dataIn.readLong()
            val mime = dataIn.readUTF()

            val outDirPath = createOutDirPerMimeType(mime)
            val outDir = File(outDirPath)
            if (!outDir.exists())outDir.mkdirs()

            val outFile = File(outDir,fileName)

            val buffer = ByteArray(4*1024)

            transferListener?.onStartReceive(i,fileName,outFile.absolutePath,mime,fileLength)

            FileOutputStream(outFile).use {

                while (fileLength >0){
                    val receiveSize = if (fileLength > buffer.size) buffer.size else fileLength
                    val receivedBytes = dataIn.read(buffer,0, receiveSize.toInt())

                    fileLength-=receivedBytes

                    it.write(buffer,0,receivedBytes)

                    transferListener?.onBytesReceived(i,receivedBytes.toLong())
                }
            }

            transferListener?.onEndReceive(i)
        }

        dataIn.close()
        dataOut.close()
        sessionState = SessionState.ENDED
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