package com.a.freeshare.fragment.media

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.SharedPrefConstants
import com.a.freeshare.activity.SelectActivity
import com.a.freeshare.adapter.FileItemRecyclerAdapter
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.impl.OnItemClickListener
import com.a.freeshare.obj.FileItem
import com.a.freeshare.source.LocalMediaSourceViewModel
import java.io.File

abstract class MediaListCommonFragment :BaseFragment(),CommonSelectionImpl<FileItem>{

    @IdRes
    protected var sortId:Int? = null
    protected var scrollPosition:Int = 0

    open lateinit var dAdapter:FileItemRecyclerAdapter
    protected val mediaSourceViewModel by lazy {
        ViewModelProvider(this).get(LocalMediaSourceViewModel::class.java)
    }
    protected lateinit var selection:HashMap<Long,Int>

    protected var lType = FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR
    protected lateinit var mLayoutManager:RecyclerView.LayoutManager

    protected  val sp by lazy {
        requireActivity().getSharedPreferences(SharedPrefConstants.SETTINGS_PREF, Context.MODE_PRIVATE)
    }

    abstract override fun hasCleared(): Boolean
    abstract fun getMediaRecycler():RecyclerView?

    override fun onCreate(savedInstanceState: Bundle?) {
        if (this is CommonSelectionImpl<*>){
            savedInstanceState?.getInt(SCROLL_POSITION, scrollPosition)?.also {
                scrollPosition = it
            }
        }


        sortId = sp.getInt(SharedPrefConstants.SORT_ORDER_IMAGE, R.id.sort_by_new)
        if (savedInstanceState == null) {

            selection = hashMapOf()

        } else{

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                selection =  (savedInstanceState.getSerializable(SELECTION_HASH_MAP,
                    HashMap::class.java
                ) as HashMap<Long, Int>?)!!
            }else{
                selection = (savedInstanceState.getSerializable(SELECTION_HASH_MAP) as HashMap<Long, Int>)

            }

            lType = savedInstanceState.getInt(LAYOUT_TYPE)
        }
        dAdapter = FileItemRecyclerAdapter(arrayListOf(),lType).apply {
            setSelection(selection)
        }

        mLayoutManager = if (lType == FileItemRecyclerAdapter.LAYOUT_TYPE_GRID){
            GridLayoutManager(requireActivity(),3)
        }else{
            LinearLayoutManager(requireActivity())
        }

        dAdapter.listener = object : OnItemClickListener {
            override fun onItemClick(v: View?, itemPosition: Int, vh: RecyclerView.ViewHolder) {

                val clickedItem = mediaSourceViewModel.fileMediaArr.value!![itemPosition]

                if (this@MediaListCommonFragment is FileTreeFragmentList && File(clickedItem.absPath).isDirectory){

                    if (File(clickedItem.absPath).isDirectory){

                        directoryStack.push(clickedItem.absPath)
                        if (visited.contains(directoryStack.peek())){

                            mediaSourceViewModel.fileMediaArr.postValue(visited.get(directoryStack.peek()))
                        }else{
                            mediaSourceViewModel.loadDirectoryContent(clickedItem.absPath!!,sortId)
                        }
                    }else{

                        dAdapter.toggleSelection(clickedItem)

                        if (requireActivity() is SelectActivity) (requireActivity() as SelectActivity).setSelectionStates()
                    }
                }else{
                    dAdapter.toggleSelection(clickedItem)
                    if (requireActivity() is SelectActivity) (requireActivity() as SelectActivity).setSelectionStates()

                }

            }
        }


        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (this is CommonSelectionImpl<*>){ scroll() }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putSerializable(SELECTION_HASH_MAP,getSelectionHashMap())
        (view as RecyclerView).layoutManager?.onSaveInstanceState()
        outState.putInt(LAYOUT_TYPE,dAdapter.mViewType)

        getMediaRecycler()?.also {
            outState.putInt(
                SCROLL_POSITION,
                (it.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            )
        }
        super.onSaveInstanceState(outState)

    }

    override fun onPause() {
        super.onPause()
        sp.edit().commit()
    }

    override fun sort(@IdRes sortType:Int){
        sortId = sortType
        LocalMediaSourceViewModel.sort(sortType,true,mediaSourceViewModel.fileMediaArr)
        sp.edit {
            putInt(SharedPrefConstants.SORT_ORDER_IMAGE,sortType)
            apply()
        }
    }

    override fun toggleGridLayout() {
        selection = dAdapter.getSelection()
        scrollPosition = (if (mLayoutManager is LinearLayoutManager){(mLayoutManager as LinearLayoutManager)} else (mLayoutManager as GridLayoutManager)).findLastVisibleItemPosition()

        if (dAdapter.mViewType == FileItemRecyclerAdapter.LAYOUT_TYPE_GRID){
            dAdapter = FileItemRecyclerAdapter(mediaSourceViewModel.fileMediaArr.value!!,FileItemRecyclerAdapter.LAYOUT_TYPE_LINEAR).apply {
                setSelection(selection)
            }

            mLayoutManager = LinearLayoutManager(requireActivity())

        }else{
            dAdapter = FileItemRecyclerAdapter(mediaSourceViewModel.fileMediaArr.value!!,FileItemRecyclerAdapter.LAYOUT_TYPE_GRID).apply {
                setSelection(selection)
            }

            mLayoutManager = GridLayoutManager(requireActivity(),3)

        }

        lType = dAdapter.mViewType

        getMediaRecycler()?.apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }

        scroll()
        dAdapter.listener = object : OnItemClickListener {
            override fun onItemClick(v: View?, itemPosition: Int, vh: RecyclerView.ViewHolder) {
                val clickedItem = mediaSourceViewModel.fileMediaArr.value!![itemPosition]

                if (this@MediaListCommonFragment is FileTreeFragmentList && File(clickedItem.absPath).isDirectory){

                    if (File(clickedItem.absPath).isDirectory){

                        directoryStack.push(clickedItem.absPath)
                        if (visited.contains(directoryStack.peek())){

                            mediaSourceViewModel.fileMediaArr.postValue(visited.get(directoryStack.peek()))
                        }else{
                            mediaSourceViewModel.loadDirectoryContent(clickedItem.absPath!!,sortId)
                        }
                    }else{

                        dAdapter.toggleSelection(clickedItem)

                        if (requireActivity() is SelectActivity) (requireActivity() as SelectActivity).setSelectionStates()
                    }
                }else{
                    dAdapter.toggleSelection(clickedItem)

                    if (requireActivity() is SelectActivity) (requireActivity() as SelectActivity).setSelectionStates()
                }

            }
        }

    }

    override fun isGridLayout(): Boolean {
        return dAdapter.mViewType == FileItemRecyclerAdapter.LAYOUT_TYPE_GRID
    }

    override fun getTotalItems(): List<FileItem> {
        return mediaSourceViewModel.fileMediaArr.value!!
    }

    override fun getSelectedItems(): List<FileItem> {
        return dAdapter.getSelectedItems()
    }

    override fun getSelectionHashMap(): HashMap<Long, Int> {
        return dAdapter.getSelection()
    }

    override fun clearSelection() {

        dAdapter.clearSelection()
    }

    override fun selectInverse() {
        dAdapter.selectInverse()
    }

    override fun selectAll() {

        dAdapter.selectAll()
    }


    protected fun scroll(){

        getMediaRecycler()?.apply {

            post {
                scrollToPosition(scrollPosition)
            }
        }

    }

    fun getSortTypeId():Int?
    {
        return sortId
    }


}