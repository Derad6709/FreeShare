package com.a.freeshare.fragment.search

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.activity.SelectActivity
import com.a.freeshare.fragment.media.AppsFragmentList
import com.a.freeshare.fragment.media.FileTreeFragmentList
import com.a.freeshare.fragment.media.MediaListCommonFragment
import com.a.freeshare.fragment.media.PhotosFragmentList
import com.a.freeshare.fragment.media.SongsFragmentList
import com.a.freeshare.fragment.media.VideosFragmentList
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.FileItem
import com.a.freeshare.source.LocalMediaSourceViewModel
import java.io.File


class SearchResultFragmentList : MediaListCommonFragment() {

    open class HashTagStore(private val tag:String,private val hash:HashMap<Long,Int>){

        class RangeStore(private val tag:String,private val start:Int,private val end:Int){

            fun isPresent(index:Int):Boolean = index in start .. end
        }
    }

    companion object{
        const val TAG = "SearchResultFragmentList"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_media,container,false)
    }

    private var imageListRange = HashTagStore.RangeStore(TAG,0,0,)
    private var videoListRange = HashTagStore.RangeStore(TAG,0,0,)
    private var songListRange = HashTagStore.RangeStore(TAG,0,0,)
    private var fileListRange = HashTagStore.RangeStore(TAG,0,0,)
    private var appListRange = HashTagStore.RangeStore(TAG,0,0,)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        dAdapter.setSelection(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(SELECTION_HASH_MAP,HashMap::class.java) as HashMap<Long, Int>
        }else{
            arguments?.getSerializable(SELECTION_HASH_MAP) as HashMap<Long, Int>
        })

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner) {

            dAdapter.setDataAndNotify(it)
        }

        (view as RecyclerView).apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = dAdapter
        }

        dAdapter.listener = object :OnItemClickListener{

            override fun onItemClick(v: View?, itemPosition: Int, vh: RecyclerView.ViewHolder) {
                val clickedItem = mediaSourceViewModel.fileMediaArr.value!![itemPosition]

                dAdapter.toggleSelection(clickedItem)

                var tag = TAG;

                for(rs in getRanges()){
                    if(rs.isPresent(itemPosition))tag = rs.tag
                }

                requireActivity().supportFragmentManager.findFragmentByTag(tag).also{it->
                    if(it != null && it is MediaListCommonFragment){
                        (it as MediaListCommonFragment).dAdapter.toggleSelection(clickedItem)
                    }
                }
            }
        }
    }



    fun filter(arg:String){

        if (arg.isEmpty()) {
            mediaSourceViewModel.fileMediaArr.value = arrayListOf()
        }else{

            Handler(Looper.getMainLooper()).post {

                Thread{
                    val arr = ArrayList<FileItem>()

                    for (f in requireActivity().supportFragmentManager.fragments){

                        if (f is MediaListCommonFragment && f !is SearchResultFragmentList){

                            val startRange = arr.size

                            for ( data in f.getTotalItems()){
                                if (data.name.contains(arg,true))arr.add(data)
                            }

                            val endRange = arr.size

                            when{

                                f is PhotosFragmentList->{
                                    imageListRange = HashTagStore.RangeStore(PhotosFragmentList.TAG!!,startRange,endRange)
                                }

                                f is VideosFragmentList->{
                                    videoListRange = HashTagStore.RangeStore(VideosFragmentList.TAG!!,startRange,endRange)
                                }

                                f is SongsFragmentList->{
                                    songListRange = HashTagStore.RangeStore(SongsFragmentList.TAG!!,startRange,endRange)
                                }

                                f is FileTreeFragmentList->{
                                    fileListRange = HashTagStore.RangeStore(FileTreeFragmentList.TAG!!,startRange,endRange)
                                }

                                f is AppsFragmentList->{
                                    appListRange = HashTagStore.RangeStore(AppsFragmentList.TAG,startRange,endRange)
                                }
                            }
                        }
                    }

                    arr.addAll(LocalMediaSourceViewModel.getFileItemsFromDirRecursive(Environment.getExternalStorageDirectory().absolutePath,"N/A",arr,arg))
                    mediaSourceViewModel.fileMediaArr.postValue(arr)
                }.start()
            }
        }

    }


    fun getRanges():ArrayList<HashTagStore.RangeStore>{

        return arrayListOf(imageListRange,videoListRange,songListRange,fileListRange,appListRange)
    }

    override fun hasCleared(): Boolean {
        return true
    }

    override fun getTotalItems(): List<FileItem> {
        return  listOf()
    }

    override fun getMediaRecycler(): RecyclerView? {
        return view as RecyclerView
    }
}
