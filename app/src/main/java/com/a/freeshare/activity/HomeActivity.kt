package com.a.freeshare.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.AppConstants
import com.a.freeshare.R
import com.a.freeshare.adapter.ShareRecyclerViewAdapter
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.HelperItem
import com.a.freeshare.util.PermissionWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.EdgeToEdgeUtils
import java.io.File
import java.lang.Exception
import java.net.URLConnection
import kotlin.system.exitProcess


class HomeActivity : AppCompatActivity() {

    private lateinit var btnSend:Button
    private lateinit var btnReceive:Button

    companion object{
       private const val PERMISSIONS_RESOLVE_REQUEST_CODE:Int = 1000
        private const val TAG = "HomeActivity"
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.applyEdgeToEdge(window,true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //when there is no support for wifi direct
        packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT).also{
            if (!it){

                val dialog = MaterialAlertDialogBuilder(this).setTitle("Error")
                    .setMessage("This device doesn't support this app")
                    .setPositiveButton("Close"
                    ) { p0, p1 -> exitProcess(0) }.setCancelable(false)

                dialog.show()
            }
        }

        btnSend = findViewById<Button?>(R.id.activity_home_btnSend).also {

            it.setOnClickListener {
                resolveSendOrReceiveAction(SelectActivity::class.simpleName!!)
            }
        }

        btnReceive = findViewById<Button?>(R.id.activity_home_btnReceive).also {
            it.setOnClickListener {

               resolveSendOrReceiveAction(SessionActivity::class.simpleName!!)
            }
        }

        val map = URLConnection.getFileNameMap()
        val arr = ArrayList<HelperItem>()
        val sharedPreferences = getSharedPreferences("History",Context.MODE_PRIVATE)

        for (entry in sharedPreferences.all){
            var f:File? = null

            try {
                f =  File(entry.value as String)
            }catch (e:Exception){
                e.printStackTrace()
            }

            val h = HelperItem(entry.key as String,entry.value as String,null,map.getContentTypeFor(entry.key),
                f?.length() ?: 0L,HelperItem.ItemState.ENDED,HelperItem.ItemState.RECEIVED)
            arr.add(h)


        }

        findViewById<RecyclerView>(R.id.activity_home_history_recycler).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = ShareRecyclerViewAdapter(arr,null,object :OnItemClickListener{

                override fun onItemClick(v: View?, itemPosition: Int, vh: RecyclerView.ViewHolder) {

                    val a = arr[itemPosition]

                    if (a.absPath !="N/A"){

                        val f = File(a.absPath)
                        val u = FileProvider.getUriForFile(this@HomeActivity,"com.a.freeshare.fileprovider",f)

                        val open = Intent(Intent.ACTION_VIEW)
                        open.setDataAndType(u,a.mime)
                        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val chooser = Intent.createChooser(open,getString(R.string.open_with))
                        startActivity(chooser)
                    }
                }
            })

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

            PERMISSIONS_RESOLVE_REQUEST_CODE->{

              if (resultCode  == RESULT_OK){

                  val clazz = Class.forName(data?.getStringExtra(AppConstants.CLASS_NAME)!!)
                  val intent = Intent(this,clazz)
                  if (clazz.simpleName == SessionActivity::class.simpleName){
                      intent.putExtra(SessionActivity.SESSION_TYPE,SessionActivity.SESSION_TYPE_RECEIVE)
                  }
                  startActivity(intent)
              }
            }

        }
    }

    private fun resolveSendOrReceiveAction(forwardComponentName : String){

        if (forwardComponentName.equals(SessionActivity::class.simpleName)){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                if(!PermissionWrapper.hasStorageAccess(this) || !PermissionWrapper.hasNearbyDevicesAccess(this) || !PermissionWrapper.hasPostNotificationAccess(this)){

                    val i = Intent(this,PermissionActivity::class.java)
                    i.putExtra(AppConstants.CLASS_NAME,SessionActivity::class.qualifiedName)

                    startActivityForResult(i, PERMISSIONS_RESOLVE_REQUEST_CODE)
                }else{
                    startActivity(Intent(this,SessionActivity::class.java).apply {
                        putExtra(SessionActivity.SESSION_TYPE,SessionActivity.SESSION_TYPE_RECEIVE)
                    })
                }
            }else{

                if(!PermissionWrapper.hasStorageAccess(this) || !PermissionWrapper.hasLocationAccess(this)){

                    val i = Intent(this,PermissionActivity::class.java)
                    i.putExtra(AppConstants.CLASS_NAME,SessionActivity::class.qualifiedName)

                    startActivityForResult(i, PERMISSIONS_RESOLVE_REQUEST_CODE)
                }else{
                    startActivity(Intent(this,SessionActivity::class.java).apply {
                        putExtra(SessionActivity.SESSION_TYPE,SessionActivity.SESSION_TYPE_RECEIVE)
                    })
                }
            }

        }else if (forwardComponentName.equals(SelectActivity::class.simpleName)){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                if(!PermissionWrapper.hasStorageAccess(this) || !PermissionWrapper.hasNearbyDevicesAccess(this) || !PermissionWrapper.hasPostNotificationAccess(this)){

                    val i = Intent(this,PermissionActivity::class.java)
                    i.putExtra(AppConstants.CLASS_NAME,SelectActivity::class.qualifiedName)

                    startActivityForResult(i, PERMISSIONS_RESOLVE_REQUEST_CODE)
                }else{
                    startActivity(Intent(this,SelectActivity::class.java))

                }
            }else{

                if(!PermissionWrapper.hasStorageAccess(this) || !PermissionWrapper.hasLocationAccess(this)){

                    val i = Intent(this,PermissionActivity::class.java)
                    i.putExtra(AppConstants.CLASS_NAME,SelectActivity::class.qualifiedName)

                    startActivityForResult(i, PERMISSIONS_RESOLVE_REQUEST_CODE)
                }else{
                    startActivity(Intent(this,SelectActivity::class.java))

                }
            }
        }

    }

}