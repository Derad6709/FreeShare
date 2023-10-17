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

class SongsFragmentList :
   MediaListCommonFragment() {

    companion object {

        val TAG = SongsFragmentList::class.simpleName
        @JvmStatic
        fun newInstance() =
            SongsFragmentList().apply {
                arguments = Bundle().apply {

                }
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

        (view as RecyclerView).apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){

            dAdapter.setDataAndNotify(it)

        }

        if (savedInstanceState == null)mediaSourceViewModel.loadData(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.AudioColumns._ID,
            MediaStore.Audio.AudioColumns.DATA,
            null, null,
        sortId!!)


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