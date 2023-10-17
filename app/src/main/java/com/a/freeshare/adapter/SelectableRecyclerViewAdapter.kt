package com.a.freeshare.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.obj.FileItem


abstract class SelectableRecyclerViewAdapter<ITEM,VH : RecyclerView.ViewHolder>(items: ArrayList<ITEM>) :
    RecyclerView.Adapter<VH>() {

    protected var items:ArrayList<ITEM> = items
        get() = field

    private var selectedItemsHashes:ArrayList<Long>
    private var isSingleSelection:Boolean = false

    fun setSingleSelection(singleSelection:Boolean){
        isSingleSelection = singleSelection
    }

    fun isSingleSelection():Boolean{
        return isSingleSelection
    }

    init {
        selectedItemsHashes = ArrayList()
    }

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH
    abstract override fun onBindViewHolder(holder: VH, position: Int)

    override fun getItemCount(): Int = items.size

    fun toggleSelection(o:ITEM){
        if (!isSingleSelection){
            if (selectedItemsHashes.contains(o.hashCode().toLong())){
                selectedItemsHashes.remove(o.hashCode().toLong())
            }else{
                selectedItemsHashes.add(o.hashCode().toLong())
            }

            notifyItemChanged(items.indexOf(o))
        }else{

            if (selectedItemsHashes.contains(o.hashCode().toLong())){
                selectedItemsHashes.remove(o.hashCode().toLong())
                notifyItemChanged(items.indexOf(o))
            }else{
                for (item in items){
                    if (selectedItemsHashes.contains(item.hashCode().toLong())){
                        selectedItemsHashes.remove(item.hashCode().toLong())
                        notifyItemChanged(items.indexOf(item))
                    }
                }
                selectedItemsHashes.add(o.hashCode().toLong())
                notifyItemChanged(items.indexOf(o))
            }


        }
    }

    fun getSelection():ArrayList<Long>{
        return selectedItemsHashes
    }

    fun setSelection(newSelection:ArrayList<Long>){
        selectedItemsHashes = newSelection
    }

    fun isSelected(id:Long):Boolean{
        return selectedItemsHashes.contains(id)
    }

    fun getSelectedItems():ArrayList<ITEM>{

        val tmp = arrayListOf<ITEM>().also {
            for (item in items){
                if (selectedItemsHashes.contains(item.hashCode().toLong()))it.add(item)
            }
        }

        return tmp
    }

    fun setDataAndNotify(newData : ArrayList<ITEM>){
        items = newData

        notifyDataSetChanged()
    }

    fun clearSelection(){
        selectedItemsHashes.clear()

    }
}