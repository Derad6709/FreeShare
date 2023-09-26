package com.a.freeshare.fragment.media

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.source.LocalMediaSourceViewModel

class VideosFragment :
    BaseFragment(), CommonSelectionImpl<FileItem> {

    companion object {

        val TAG = VideosFragment::class.simpleName

    }

    private lateinit var dAdapter:FileItemRecyclerAdapter
    private val mediaSourceViewModel by lazy {
        ViewModelProvider(this).get(LocalMediaSourceViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null)mediaSourceViewModel.loadData(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.VideoColumns._ID,
            MediaStore.Video.VideoColumns.DATA,
            null,null)

        dAdapter = if (savedInstanceState == null) {

            FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_GRID)
        } else{

            val selection = savedInstanceState.getLongArray(SELECTION_HASH_ARRAY)
            FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_GRID).apply {
                setSelection(ArrayList<Long>().apply { addAll(selection!!.toList()) })
            }
        }


        (view as RecyclerView).apply {
            layoutManager = GridLayoutManager(requireActivity(),3)
            adapter = dAdapter
        }
        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){
            dAdapter.setDataAndNotify(it)
        }
    }

    override fun getSelectedItems(): List<FileItem> {
        return dAdapter.getSelectedItems()
    }

    override fun getTotalItems(): List<FileItem> {
        return mediaSourceViewModel.fileMediaArr.value!!
    }

    override fun getSelection(): ArrayList<Long> {
        return dAdapter.getSelection()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(SELECTION_HASH_ARRAY,getSelection().toLongArray())
    }

    override fun hasCleared(): Boolean {
        return true
    }

}