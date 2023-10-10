package com.a.freeshare.fragment.media

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.source.LocalMediaSourceViewModel

class SongsFragment :
    BaseFragment(), CommonSelectionImpl<FileItem> {

    companion object {

        val TAG = SongsFragment::class.simpleName
        @JvmStatic
        fun newInstance() =
            SongsFragment().apply {
                arguments = Bundle().apply {

                }
            }
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

        if (savedInstanceState == null)mediaSourceViewModel.loadData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,MediaStore.Audio.AudioColumns._ID,MediaStore.Audio.AudioColumns.DATA,Environment.getExternalStorageDirectory().absolutePath,"audio/")

        dAdapter = if (savedInstanceState == null) {

            FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR)
        } else{

            val selection = savedInstanceState.getLongArray(SELECTION_HASH_ARRAY)
            FileItemRecyclerAdapter(arrayListOf(),FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR).apply {
                setSelection(ArrayList<Long>().apply { addAll(selection!!.toList()) })
            }
        }

        (view as RecyclerView).apply {
            layoutManager = LinearLayoutManager(requireActivity())
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

    override fun clearSelection() {
        val tempSelected:ArrayList<Long> = dAdapter.getSelection().clone() as ArrayList<Long>
        dAdapter.clearSelection()
        for (o in 0 until mediaSourceViewModel.fileMediaArr.value!!.size){
            if (tempSelected.contains(dAdapter.getItemId(o))){
                dAdapter.notifyItemChanged(o)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(SELECTION_HASH_ARRAY,getSelection().toLongArray())
    }

    override fun hasCleared(): Boolean {
        return true
    }


}