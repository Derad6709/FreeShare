package com.a.freeshare.fragment.media

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import java.io.File
import java.lang.StringBuilder
import java.net.URLConnection
import kotlin.math.abs

class AppsFragment() : BaseFragment(), CommonSelectionImpl<FileItem> {

    companion object {
        val TAG = AppsFragment::class.simpleName
    }

    private val fileNameMap by lazy {
        URLConnection.getFileNameMap()
    }

    private lateinit var dAdapter: FileItemRecyclerAdapter
    private lateinit var items:ArrayList<FileItem>

    override fun hasCleared(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_media,container,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {

            items = ArrayList()
            dAdapter = FileItemRecyclerAdapter(items,FileItemRecyclerAdapter.LAYOUT_TYPE_GRID)
        } else{

            val selection = savedInstanceState.getLongArray(SELECTION_HASH_ARRAY)

            items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
            } else {
                savedInstanceState.getSerializable(ITEMS) as ArrayList<FileItem>
            }

            dAdapter = FileItemRecyclerAdapter(items,FileItemRecyclerAdapter.LAYOUT_TYPE_GRID).apply {
                setSelection(ArrayList<Long>().apply { addAll(selection!!.toList()) })
            }

        }

        (view as RecyclerView).apply {
            layoutManager = GridLayoutManager(requireActivity(),3)
            adapter = dAdapter
        }

        if (savedInstanceState == null){
            val installedAppsInfo = if (Build.VERSION.SDK_INT >= 33) {
                requireActivity().packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            }else{
                requireActivity().packageManager.getInstalledApplications(0)
            }

            Thread{

                for (app in installedAppsInfo){

                    if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0){

                        val name = "${app.loadLabel(requireActivity().packageManager)}.apk"
                        val absPath = app.publicSourceDir
                        val apkFile = File(absPath)
                        val dataSize = apkFile.length()
                        val mime = fileNameMap.getContentTypeFor(File(absPath).name)

                        items.add(FileItem(name,absPath,dataSize,apkFile.lastModified(),mime))
                    }
                }

                view.post {
                    dAdapter.setDataAndNotify(items)
                }
            }.start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLongArray(SELECTION_HASH_ARRAY,getSelection().toLongArray())
        outState.putSerializable(ITEMS,items)

    }

    override fun getSelectedItems(): List<FileItem> {
        return dAdapter.getSelectedItems()
    }

    override fun getTotalItems(): List<FileItem> {
        return items
    }

    override fun getSelection(): ArrayList<Long> {
        return dAdapter.getSelection()
    }
}