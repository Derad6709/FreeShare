package com.a.freeshare.fragment.media

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import java.net.URLConnection

class FilesFragment :
    BaseFragment(),CommonSelectionImpl<FileItem>{

    companion object {

        val TAG = FilesFragment::class.simpleName
        @JvmStatic
        fun newInstance() =
            FilesFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    private var array:ArrayList<FileItem>? = null
    private lateinit var fragment: FileTreeFragment

    private val fileNameMap by lazy {
        URLConnection.getFileNameMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_files, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       val spinner = view.findViewById<Spinner>(R.id.spinner)
        val arrayAdaptor = ArrayAdapter(requireActivity(),android.R.layout.simple_list_item_1,
            listOf("Internal storage","SD CARD"))
        spinner.adapter = arrayAdaptor

         fragment = if (savedInstanceState == null){
            FileTreeFragment().apply {
                arguments = Bundle().apply {
                    putString(FileTreeFragment.SRC_DIR_PATH,Environment.getExternalStorageDirectory().absolutePath)
                }
            }

        }else {
            childFragmentManager.findFragmentByTag(FileTreeFragment.TAG) as FileTreeFragment
        }

        if (savedInstanceState == null)
        childFragmentManager.commit {
            add(R.id.fragment_files_holder,fragment, FileTreeFragment.TAG)
        }
    }

    override fun hasCleared(): Boolean {
        return fragment.hasCleared()
    }

    override fun getTotalItems(): List<FileItem> {
        return fragment.getTotalItems()
    }

    override fun getSelectedItems(): List<FileItem> {
        return fragment.getSelectedItems()
    }

    override fun getSelection(): ArrayList<Long> {
        return fragment.getSelection()
    }
}