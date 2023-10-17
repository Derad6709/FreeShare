package com.a.freeshare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.a.freeshare.impl.SocketListener
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem

class SocketTransferService : Service(),SocketListener,TransferImpl,Session.SessionImpl {

    private lateinit var session: Session

    private var isServiceRunning = false

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notification: Notification

    private var socketListener: SocketListener? = null
    private var transferImpl: TransferImpl? = null
    private var sessionImpl:Session.SessionImpl? = null

    private var host:String? = null

    var isSessionRunning:Boolean = false

    companion object{
        const val NOTIFICATION_CHANNEL_ID = "SocketTransferService"
        const val NOTIFICATION_CHANNEL_NAME = "SocketTransferServiceChannel"

        const val EXTRA_HOST = "SocketTransferService_extra_host"

        const val FOREGROUND_ID = 1001
    }

    inner class InnerBinder:Binder(){

        fun getService():SocketTransferService = this@SocketTransferService
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,NotificationManager.IMPORTANCE_HIGH)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("${getString(R.string.app_name)} ${getString(R.string.is_running)}")
            .setContentText(getString(R.string.do_not_close_app))
            .setOngoing(true)
        notification = builder.build()

        startForeground(FOREGROUND_ID,notification)

    }

    override fun onBind(p0: Intent?): IBinder {

        return InnerBinder()
    }

    fun setSocketListener(socketListener:SocketListener){
        this.socketListener = socketListener
    }

    fun setTransferImpl(transferImpl: TransferImpl){
        this.transferImpl = transferImpl
    }

    fun setSessionImpl(sessionImpl: Session.SessionImpl){
        this.sessionImpl = sessionImpl
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        host = intent?.getStringExtra(EXTRA_HOST)

        session = if (host != null){
            Session(host!!,applicationContext,this,this,this)

        }else{
            Session(applicationContext,this,this,this)
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        stopServiceAndHaltOperation()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopServiceAndHaltOperation()
    }

    fun stopSessionAndService(){
       stopServiceAndHaltOperation()
    }

    private fun stopServiceAndHaltOperation(){
        session.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }else{
            stopForeground(true)
        }
        stopSelf()
    }

    fun startSession(){
        session.startSession()
    }

    override fun onSocket() {
        socketListener?.onSocket()
    }

    fun addToCancelIndex(at:Int) { session.addToCancelIndex(at) }

    fun send(fileItems:List<FileItem>){
        session.send(fileItems)
    }

    fun sendReceiveCommand(fileItems: List<FileItem>){
        session.sendReceiveCommand(fileItems)
    }

    fun receive(){
        session.receive()
    }

    @Synchronized
    override fun onSendFiles(startPosition: Int, count: Int, files: List<FileItem>) {
        transferImpl?.onSendFiles(startPosition, count, files)
    }

    @Synchronized
    override fun onStartSend(
        index: Int,
        name: String,
        absPath: String,
        mime: String?,
        length: Long
    ) {
        Log.d("SessionSocketService","SocketTransferService onStart()")
        transferImpl?.onStartSend(index, name, absPath, mime, length)

    }

    @Synchronized
    override fun onBytesSent(index: Int, bytes: Long) {
        Log.d("SessionSocketService","SocketTransferService onBytes()")
      transferImpl?.onBytesSent(index,bytes)
    }

    @Synchronized
    override fun onEndSend(index: Int) {
      transferImpl?.onEndSend(index)
    }

    @Synchronized
    override fun onReceiveFiles(startPosition: Int, count: Int) {
        transferImpl?.onReceiveFiles(startPosition, count)
    }

    @Synchronized
    override fun onStartReceive(
        index: Int,
        name: String,
        absPath: String,
        mime: String?,
        length: Long
    ) {
        transferImpl?.onStartReceive(index, name, absPath, mime, length)
    }

    @Synchronized
    override fun onBytesReceived(index: Int, bytes: Long) {
       transferImpl?.onBytesReceived(index, bytes)
    }

    @Synchronized
    override fun onEndReceive(index: Int) {
      transferImpl?.onEndReceive(index)
    }

    @Synchronized
    override fun onSkipped(index: Int) {
        transferImpl?.onSkipped(index)
    }

    @Synchronized
    override fun onStarted() {
        sessionImpl?.onStarted()
    }

    @Synchronized
    override fun onEnded() {
        sessionImpl?.onEnded()
    }
}