package com.a.freeshare.activity

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.a.freeshare.AppConstants
import com.a.freeshare.R
import com.a.freeshare.util.PermissionWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.StringBuilder
import java.lang.reflect.Method
import kotlin.system.exitProcess


class HomeActivity : AppCompatActivity() {

    private lateinit var btnSend:Button
    private lateinit var btnReceive:Button

    companion object{
       private const val PERMISSIONS_RESOLVE_REQUEST_CODE:Int = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
                if(!PermissionWrapper.hasStorageAccess(this) || !PermissionWrapper.hasLocationAccess(this)){

                    val i = Intent(this,PermissionActivity::class.java)
                    i.putExtra(AppConstants.CLASS_NAME,SelectActivity::class.qualifiedName)

                    startActivityForResult(i, PERMISSIONS_RESOLVE_REQUEST_CODE)
                }else{
                    startActivity(Intent(this,SelectActivity::class.java))
                }
            }
        }

        btnReceive = findViewById<Button?>(R.id.activity_home_btnReceive).also {
            it.setOnClickListener {

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
}