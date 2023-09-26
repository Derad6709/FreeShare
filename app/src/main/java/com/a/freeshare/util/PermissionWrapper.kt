package com.a.freeshare.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

class PermissionWrapper {

    companion object{

        fun hasStorageAccess(ctx: Context):Boolean{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                return Environment.isExternalStorageManager()
            }else{
                val read = ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_EXTERNAL_STORAGE)
                val write = ContextCompat.checkSelfPermission(ctx,Manifest.permission.WRITE_EXTERNAL_STORAGE)

                return read == PackageManager.PERMISSION_GRANTED && (write == PackageManager.PERMISSION_GRANTED)
            }
        }

        fun hasLocationAccess(ctx: Context):Boolean{
            return ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}