package com.a.freeshare.fragment.media

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import java.io.File
import java.net.URLConnection
import java.util.*
import kotlin.collections.ArrayList


class FileTreeFragment : BaseFragment(), CommonSelectionImpl<FileItem> {

    companion object {

        const val SRC_DIR_PATH = "source_directory_path"
        private const val DIR_STACK = "directory_stack"

        val TAG = FileTreeFragment::class.simpleName

    }

    private val fileNameMap by lazy {
        URLConnection.getFileNameMap()
    }

    private lateinit var sourceDirPath :String
    private lateinit var directoryStack : Stack<String>
    private lateinit var items: ArrayList<FileItem>

    private lateinit var dAdapter:FileItemRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
               sourceDirPath = it.getString(SRC_DIR_PATH)!!
        }

        if (savedInstanceState == null) {
            directoryStack = Stack<String>().apply {
                push(sourceDirPath)
            }
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                directoryStack = savedInstanceState.getSerializable(DIR_STACK,Stack::class.java) as Stack<String>
            } else {
                directoryStack = savedInstanceState.getSerializable(DIR_STACK) as Stack<String>
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        dAdapter = if (savedInstanceState==null) {

            items = arrayListOf()
            FileItemRecyclerAdapter(items,FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR)
        } else{

            val selection = savedInstanceState.getLongArray(SELECTION_HASH_ARRAY)

            items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable(ITEMS,ArrayList::class.java) as ArrayList<FileItem>
            } else {
                savedInstanceState.getSerializable(ITEMS) as ArrayList<FileItem>
            }
            Toast.makeText(requireActivity(),"${selection!!.size}",Toast.LENGTH_SHORT).show()

            FileItemRecyclerAdapter(items,FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR).apply {
                setSelection(ArrayList<Long>().apply { addAll(selection!!.toList()) })
            }
        }

        (view as RecyclerView).apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = dAdapter


        }

        if (savedInstanceState == null)
        Thread{
            File(directoryStack.peek()).listFiles()?.also {
                for (f in it){
                    val mime = fileNameMap.getContentTypeFor(f.name)
                    items.add(FileItem(f,mime))
                }
            }

            view.post {
                dAdapter.setDataAndNotify(items)
            }
        }.start()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(DIR_STACK,directoryStack)
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

    override fun clearSelection() {
        val tempSelected:ArrayList<Long> = dAdapter.getSelection().clone() as ArrayList<Long>
        dAdapter.clearSelection()
        for (o in 0 until items.size){
            if (tempSelected.contains(dAdapter.getItemId(o))){
                dAdapter.notifyItemChanged(o)
            }
        }
    }


    override fun hasCleared(): Boolean {

        return true
    }
}