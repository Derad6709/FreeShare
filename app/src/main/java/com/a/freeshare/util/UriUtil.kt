package com.a.freeshare.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.lang.Exception
import kotlin.reflect.typeOf

class UriUtil {

    companion object{

        fun getFilePathFromUri(context:Context,uri: Uri):String?{
            var filePath:String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && uri.authority != null && uri.authority.equals("${context.packageName}.fileprovider")){
                filePath = getFilePathFromFileProvider(uri)
            }else if (isDocumentUri(context,uri)){
                filePath = getDocumentPath(context, uri)
            }else{
                filePath = getDataColumn(context,uri,null,null)
            }

            return filePath
        }

        private fun isDocumentUri(context: Context,uri: Uri):Boolean{
            return DocumentsContract.isDocumentUri(context,uri)
        }

        private fun getFilePathFromFileProvider(uri: Uri):String?{

            var filePath:String? = null

            try {
                filePath = File(uri.path).absolutePath
            }catch (e:Exception){
                e.printStackTrace()
            }

            return filePath
        }

        private fun getDataColumn(context: Context,uri: Uri,selection:String?,selectionArg:Array<String>?):String?{

            var filePath:String? = null

            try {
                val c = context.contentResolver.query(uri,
                    arrayOf(MediaStore.MediaColumns.DATA),selection,selectionArg,null)

                if (c != null && c.moveToFirst()){
                    filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                }
            }finally {

            }

            return filePath
        }

        private fun getDocumentPath(context: Context,uri: Uri):String?{
            var filepath:String? = null

            val docId = DocumentsContract.getDocumentId(uri)

            if (uri.authority.equals("com.android.providers.media.documents")){
                val type = docId.split(":")[0]
                var contentUri:Uri? = null

                 if ("image".equals(type)){
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }else if ("video".equals(type)){
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }else if("audio".equals(type)){
                   contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val args = docId.split(":")[1]
                filepath = getDataColumn(context,contentUri!!,selection, arrayOf(args))
            }else if ("com.android.externalstorage.documents".equals(uri.authority)){
                if ("primary".equals(docId.split(":")[0])){
                    filepath = Environment.getExternalStorageDirectory().absolutePath.plus(docId.split(":")[1])
                }
            }else if ("com.android.providers.downloads.documents".equals(uri.authority)){

                val contentUri =ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),docId.toLong())
                filepath = getDataColumn(context,contentUri,null,null)
            }

            return filepath
        }
    }
}