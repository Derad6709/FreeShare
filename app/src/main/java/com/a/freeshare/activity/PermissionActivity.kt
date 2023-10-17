package com.a.freeshare.activity

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.a.freeshare.R
import com.a.freeshare.util.PermissionWrapper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.internal.EdgeToEdgeUtils
import java.lang.Exception

class PermissionActivity : AppCompatActivity() {

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1003
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1004
        private const val NEARBY_DEVICES_REQUEST_CODE = 1005
        private const val POST_NOTIFICATION = 1006
        private const val CAMERA_CODE = 1008

        private const val TAG = "PermissionActivity"
    }

    private var permissionsHasLocation = false
    private var permissionsHasStorage = false
    private var permissionHasNearbyDevices = false
    private var permissionHasPostNotification = false
    private var permissionHasCamera = false

    private lateinit var btnAllowLocation:MaterialButton
    private lateinit var btnAllowStorage:MaterialButton
    private lateinit var btnAllowNearbyDevices:Button
    private lateinit var btnAllowPostNotification:Button
    private lateinit var btnAllowCamera:Button

    private val backPressedCallback = object :OnBackPressedCallback(true){

        override fun handleOnBackPressed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if (permissionsHasStorage && permissionHasNearbyDevices && permissionHasPostNotification){
                    setResult(RESULT_OK,intent)
                }else{
                    setResult(RESULT_CANCELED)

                }

            }else{
                if(permissionsHasStorage && permissionsHasLocation){
                    setResult(RESULT_OK,intent)
                }else{
                    setResult(RESULT_CANCELED)

                }

            }

            finish()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.applyEdgeToEdge(window,true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        onBackPressedDispatcher.addCallback(this,backPressedCallback)

        findViewById<MaterialToolbar>(R.id.toolbar).also {
           it.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed() }
        }

        btnAllowCamera = findViewById<Button?>(R.id.activity_permission_btn_allow_camera).apply {
            setOnClickListener {
                requestPermissionIfNeeded(Manifest.permission.CAMERA, CAMERA_CODE)
            }
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO),
                            STORAGE_PERMISSION_REQUEST_CODE)
                    }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            findViewById<MaterialCardView?>(R.id.materialCardView2).also {
                it.visibility = View.GONE
            }

            findViewById<MaterialCardView?>(R.id.materialCardView3).also {
                it.visibility = View.VISIBLE
            }

            findViewById<MaterialCardView?>(R.id.materialCardView4)?.also {
                it.visibility = View.VISIBLE
            }

            btnAllowNearbyDevices = findViewById<Button?>(R.id.activity_permission_btn_allow_nearby_devices).apply {
                setOnClickListener {
                    requestPermissionIfNeeded(Manifest.permission.NEARBY_WIFI_DEVICES,
                        NEARBY_DEVICES_REQUEST_CODE)
                }
            }

            btnAllowPostNotification = findViewById<Button?>(R.id.activity_permission_btn_allow_post_notification).apply {
                setOnClickListener {
                    requestPermissionIfNeeded(Manifest.permission.POST_NOTIFICATIONS,
                        POST_NOTIFICATION)
                }
            }
        }

        permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
        permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
        permissionHasCamera  = PermissionWrapper.hasCameraAccess(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permissionHasNearbyDevices = PermissionWrapper.hasNearbyDevicesAccess(this)
            permissionHasPostNotification = PermissionWrapper.hasPostNotificationAccess(this)
        }
        setStates()
        handlePermissionsOKResult()
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

            CAMERA_CODE->{
                permissionHasCamera = grantResults[0] == PackageManager.PERMISSION_GRANTED

                setStates()
                handlePermissionsOKResult()
            }

            LOCATION_PERMISSION_REQUEST_CODE->{
                permissionsHasLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED
                permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                setStates()
                handlePermissionsOKResult()
            }

            STORAGE_PERMISSION_REQUEST_CODE->{

                permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    if (grantResults.size > 0){
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED || grantResults[2] == PackageManager.PERMISSION_GRANTED){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                                try {
                                    val i = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    startActivityForResult(i, STORAGE_PERMISSION_REQUEST_CODE)
                                }catch (e:Exception){
                                    val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    i.setData(Uri.parse("package:"+applicationContext.packageName))
                                    startActivityForResult(i, STORAGE_PERMISSION_REQUEST_CODE)
                                }
                            }
                        }
                    }else {
                        permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
                        setStates()
                        handlePermissionsOKResult()
                    }
                }

            }

            NEARBY_DEVICES_REQUEST_CODE->{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionHasNearbyDevices = PermissionWrapper.hasNearbyDevicesAccess(this)
                    setStates()
                    handlePermissionsOKResult()
                }

            }

            POST_NOTIFICATION->{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionHasPostNotification = PermissionWrapper.hasPostNotificationAccess(this)
                    setStates()
                    handlePermissionsOKResult()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)

            permissionsHasStorage = PermissionWrapper.hasStorageAccess(this)
            permissionsHasLocation = PermissionWrapper.hasLocationAccess(this)
        permissionHasCamera = PermissionWrapper.hasCameraAccess(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                permissionHasNearbyDevices = PermissionWrapper.hasNearbyDevicesAccess(this)
                permissionHasPostNotification = PermissionWrapper.hasPostNotificationAccess(this)
            }
            setStates()
            handlePermissionsOKResult()

    }

    private fun handleRequestRevokeState(tv:TextView){
        tv.also {

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

    private fun handleRequestAcceptState(btn:Button){
        btn.also {
            it.isEnabled = false
            it.text = getText(R.string.allowed)
        }
    }

    private fun handlePermissionsOKResult(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (permissionsHasStorage && permissionHasNearbyDevices && permissionHasPostNotification && permissionHasCamera){
                setResult(RESULT_OK,intent)
                finish()
            }else{
                setResult(RESULT_CANCELED)

            }

        }else{
            if(permissionsHasStorage && permissionsHasLocation && permissionHasCamera){
                setResult(RESULT_OK,intent)
                finish()
            }else{
                setResult(RESULT_CANCELED)

            }

        }
    }

    private fun setStates(){

        if (!permissionHasCamera){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_camera_detail))

        }else{
            handleRequestAcceptState(btnAllowCamera)
        }

        if (!permissionsHasStorage){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_storage_detail))
        }else{
            handleRequestAcceptState(btnAllowStorage)

        }

        if (!permissionsHasLocation){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_location_detail))
        }else{
            handleRequestAcceptState(btnAllowLocation)

        }

        if (!permissionHasNearbyDevices){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_nearby_devices_detail))
        }else{
            handleRequestAcceptState(btnAllowNearbyDevices)

        }

        if (!permissionHasPostNotification){
            handleRequestRevokeState(findViewById(R.id.activity_permission_txt_post_notification_detail))
        }else{
            handleRequestAcceptState(btnAllowPostNotification)
        }
    }
}