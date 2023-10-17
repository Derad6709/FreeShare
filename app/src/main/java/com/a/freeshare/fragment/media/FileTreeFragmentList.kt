package com.a.freeshare.fragment.media

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.obj.FileItem
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class FileTreeFragmentList : MediaListCommonFragment() {

    companion object {

        const val SRC_DIR_PATH = "source_directory_path"
        private const val DIR_STACK = "directory_stack"
        private const val DIR_VISIT_HASH = "visit_hash"

        val TAG = FileTreeFragmentList::class.simpleName

    }

    private lateinit var sourceDirPath :String
    lateinit var directoryStack : Stack<String>

    lateinit var visited:HashMap<String,ArrayList<FileItem>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
               sourceDirPath = it.getString(SRC_DIR_PATH)!!
        }

        if (savedInstanceState == null) {
            directoryStack = Stack<String>().apply {
                push(sourceDirPath)
            }

            visited = hashMapOf()
        } else {
            visited = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               savedInstanceState.getSerializable(DIR_VISIT_HASH,HashMap::class.java) as HashMap<String, ArrayList<FileItem>>
            } else {
               savedInstanceState.getSerializable(DIR_VISIT_HASH) as HashMap<String, ArrayList<FileItem>>
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                directoryStack = savedInstanceState.getSerializable(DIR_STACK,Stack::class.java) as Stack<String>
            } else {
                directoryStack = savedInstanceState.getSerializable(DIR_STACK) as Stack<String>
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaSourceViewModel.fileMediaArr.observe(viewLifecycleOwner){

            if (!visited.contains(directoryStack.peek()))visited.put(directoryStack.peek(),it)

            val selection = dAdapter.getSelection()
            dAdapter.apply {
                setSelection(selection)
            }

            dAdapter.setDataAndNotify(it)
        }

        (view as RecyclerView).apply {
            layoutManager = mLayoutManager
            adapter = dAdapter
        }

        if (savedInstanceState == null) mediaSourceViewModel.loadDirectoryContent(directoryStack.peek(),sortId)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(DIR_STACK,directoryStack)
        outState.putSerializable(SELECTION_HASH_MAP,getSelectionHashMap())
        outState.putSerializable(DIR_VISIT_HASH,visited)
    }

    override fun hasCleared(): Boolean {

        if (directoryStack.size > 1){

            directoryStack.pop()

            if (visited.contains(directoryStack.peek())){

                mediaSourceViewModel.fileMediaArr.postValue(visited.get(directoryStack.peek()))
               // Toast.makeText(requireActivity(),visited.get(directoryStack.peek())?.size.toString(),Toast.LENGTH_SHORT).show()
            }else{
                mediaSourceViewModel.loadDirectoryContent(directoryStack.peek(),sortId)
            }

            return false
        }else{
            return true
        }

    }

    override fun getSelectedItems(): List<FileItem> {
        val arr = ArrayList<FileItem>()
        for (arrValue in visited.values){
            for (item in arrValue){
                if (dAdapter.isSelected(item.hashCode().toLong()))arr.add(item)
            }
        }

        return arr
    }



    override fun getMediaRecycler(): RecyclerView? {
        return if (view is RecyclerView){
            view as RecyclerView
        }else{
            null
        }
    }
}