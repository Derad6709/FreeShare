package com.a.freeshare.fragment.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R


class PhotosFragmentList : MediaListCommonFragment() {

    companion object {
        val TAG = PhotosFragmentList::class.simpleName
    }

    private var dX  = 0f
    private var dY = 0f

    private var prevX = 0f
    private var prevY = 0f

    private var adapterInitialPos = 0
    private var adapterFinalPos = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_media, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){

            dAdapter.setDataAndNotify(it)

        }

        if (savedInstanceState == null) {

            mediaSourceViewModel.loadData(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                null,null,
                 sortId)
        }

        (view as RecyclerView).apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }


    }

    override fun getMediaRecycler(): RecyclerView? {
        return if (view is RecyclerView){
            view as RecyclerView
        }else{
            null
        }
    }

    override fun hasCleared(): Boolean {
        return true
    }
}