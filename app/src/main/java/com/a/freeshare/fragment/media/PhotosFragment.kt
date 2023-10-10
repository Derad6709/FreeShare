package com.a.freeshare.fragment.media

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.source.LocalMediaSourceViewModel


class PhotosFragment : BaseFragment() , CommonSelectionImpl<FileItem> {

    companion object {
        val TAG = PhotosFragment::class.simpleName
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
    ): View? = inflater.inflate(R.layout.fragment_media, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       if (savedInstanceState == null)mediaSourceViewModel.loadData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,MediaStore.Images.ImageColumns._ID,MediaStore.Images.ImageColumns.DATA,null,null)

        if (savedInstanceState == null) {

            dAdapter = FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_GRID)
        } else{

            val selection = savedInstanceState.getLongArray(SELECTION_HASH_ARRAY)
            //Toast.makeText(requireActivity(),"$selection", Toast.LENGTH_SHORT).show()
            dAdapter = FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_GRID).apply {
                setSelection(ArrayList<Long>().apply { addAll(selection!!.toList()) })
            }

        }


        (view as RecyclerView).apply {
            layoutManager = GridLayoutManager(requireActivity(),3)
            adapter = dAdapter

        }
        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){
           dAdapter.setDataAndNotify(it)
            (view as RecyclerView).layoutManager?.onRestoreInstanceState(savedInstanceState)
        }

    }

    fun sort(@IdRes sortType:Int){
        mediaSourceViewModel.sort(sortType)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(SELECTION_HASH_ARRAY,getSelection().toLongArray())
        (view as RecyclerView).layoutManager?.onSaveInstanceState()

    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun getTotalItems(): List<FileItem> {
        return mediaSourceViewModel.fileMediaArr.value!!.toList()
    }

    override fun getSelectedItems(): List<FileItem> {
        return dAdapter.getSelectedItems()
    }

    override fun getSelection(): ArrayList<Long> {
        return dAdapter.getSelection()
    }

    override fun clearSelection() {
        val tempSelected:ArrayList<Long> = dAdapter.getSelection().clone() as ArrayList<Long>
        dAdapter.clearSelection()
        for (o in 0 until mediaSourceViewModel.fileMediaArr.value!!.size){
            if (tempSelected.contains(dAdapter.getItemId(o))){
                dAdapter.notifyItemChanged(o)
            }
        }
    }

}