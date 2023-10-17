package com.a.freeshare.fragment.media

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R

class VideosFragmentList :
    MediaListCommonFragment() {

    companion object {

        val TAG = VideosFragmentList::class.simpleName

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

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){

            dAdapter.setDataAndNotify(it)
        }

        (view as RecyclerView).apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }

        if (savedInstanceState == null) {

            mediaSourceViewModel.loadData(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA,
                null,null,
                sortId)
        }

    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun getMediaRecycler(): RecyclerView? {
        return if (view is RecyclerView){
            view as RecyclerView
        }else{
            null
        }
    }
}