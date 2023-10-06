package com.a.freeshare.activity

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.a.freeshare.R
import com.a.freeshare.util.PermissionWrapper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.lang.Exception

class PermissionActivity : AppCompatActivity() {

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1003
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1004
    }

    private var permissionsHasLocation = false
    private var permissionsHasStorage = false

    private lateinit var btnAllowLocation:MaterialButton
    private lateinit var btnAllowStorage:MaterialButton

    private val backPressedCallback = object :OnBackPressedCallback(true){

        override fun handleOnBackPressed() {
            if (permissionsHasLocation && permissionsHasStorage){
                setResult(RESULT_OK,intent)
            }else{
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        onBackPressedDispatcher.addCallback(this,backPressedCallback)

        findViewById<MaterialToolbar>(R.id.toolbar).also {
           it.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed() }
        }

        btnAllowLocation = findViewById<MaterialButton?>(R.id.activity_permission_btn_allow_location).also {
            it.setOnClickListener {
                requestPermissionIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION,
                    LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        btnAllowStorage = findViewById<MaterialButton?>(R.id.activity_permission_btn_allow_storage).also {
            it.setOnClickListener {
                if (!PermissionWrapper.hasStorageAccess(this)){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){

                        try {
                            val i = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivityForResult(i, STORAGE_PERMISSION_REQUEST_CODE)
                        }catch (e:Exception){
                            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            i.setData(Uri.parse("package:"+applicationContext.packageName))
                            startActivityForResult(i, STORAGE_PERMISSION_REQUEST_CODE)
                        }

                    }else{
                        requestPermissionIfNeeded(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            STORAGE_PERMISSION_REQUEST_CODE)
                        requestPermissionIfNeeded(Manifest.permission.READ_EXTERNAL_STORAGE,
                            STORAGE_PERMISSION_REQUEST_CODE)
                    }
                }
            }
        }
        permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
        permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
        setStates()
    }

    private fun requestPermissionIfNeeded(accessString: String,requestCode: Int) {

        if (ActivityCompat.checkSelfPermission(this,accessString) != PackageManager.PERMISSION_GRANTED){
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,accessString)){
                ActivityCompat.requestPermissions(this,
                    arrayOf(accessString),
                    requestCode)
            }else{
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:${applicationContext.packageName}")
                i.addCategory(Intent.CATEGORY_DEFAULT)

                startActivityForResult(i, requestCode)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            LOCATION_PERMISSION_REQUEST_CODE->{
                permissionsHasLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED
                permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                setStates()

            }

            STORAGE_PERMISSION_REQUEST_CODE->{
                permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
                setStates()

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        when(requestCode){
            STORAGE_PERMISSION_REQUEST_CODE->{

                permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
                setStates()

            }

            LOCATION_PERMISSION_REQUEST_CODE->{
                permissionsHasLocation = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                setStates()

            }
        }
    }

    private fun handleRequestRevokeState(tv:TextView){
        tv.also {

          val spannable = SpannableString(it.text)
            spannable.setSpan(UnderlineSpan(),0,it.text.length,0)
            tv.text = spannable

            val blinkAnimator = ValueAnimator.ofFloat(1.0f,0.0f)
            blinkAnimator.duration = 1000
            blinkAnimator.repeatCount = 3
            blinkAnimator.repeatMode = ValueAnimator.REVERSE
            blinkAnimator.addUpdateListener { animator->
                tv.alpha = animator.animatedValue as Float
            }

            blinkAnimator.start()
        }

    }

    private fun handleRequestAcceptState(btn:MaterialButton){
        btn.also {
            it.isEnabled = false
            it.text = getText(R.string.allowed)
        }
    }

    private fun handlePermissionsOKResult(){
        if (permissionsHasStorage && permissionsHasLocation){
            setResult(RESULT_OK,intent)
            finish()
        }
    }

    private fun setStates(){

        if (!permissionsHasStorage){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_storage_detail))
        }else{
            handleRequestAcceptState(btnAllowStorage)
            handlePermissionsOKResult()
        }

        if (!permissionsHasLocation){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_location_detail))
        }else{
            handleRequestAcceptState(btnAllowLocation)
            handlePermissionsOKResult()
        }
    }
}