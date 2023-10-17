package com.a.freeshare.fragment.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R

class AppsFragmentList :MediaListCommonFragment() {

    companion object {
         const val TAG = "AppsFragmentList"

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_media,container,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){

            dAdapter.setDataAndNotify(it)
        }

        (view as RecyclerView).apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }

        if (savedInstanceState == null)mediaSourceViewModel.loadApks(sortId)
    }

}