package com.a.freeshare.adapter

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.obj.FileItem


abstract class SelectableRecyclerViewAdapter<ITEM,VH : RecyclerView.ViewHolder>(items: ArrayList<ITEM>) :
    RecyclerView.Adapter<VH>() {

    protected var items:ArrayList<ITEM> = items
        get() = field

    private var selectedItemsHashes:HashMap<Long,Int>

    private var isSingleSelection:Boolean = false

    private val vhs:ArrayList<VH> = arrayListOf()

    fun setSingleSelection(singleSelection:Boolean){
        isSingleSelection = singleSelection
    }

    fun isSingleSelection():Boolean{
        return isSingleSelection
    }

    init {
        selectedItemsHashes = hashMapOf()

    }

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH
    protected abstract fun onUpdateView(vh: VH, position: Int)
    abstract fun isSelectable(position: Int):Boolean

    private fun updateCurrentViewHolders(o: ITEM){
        var currentHolder:VH? = null
        for (vh in vhs){
            if (vh.adapterPosition == items.indexOf(o)){
                currentHolder = vh
            }
        }

        currentHolder?.also { onUpdateView(it,it.adapterPosition) }
    }

    @CallSuper
    override fun onBindViewHolder(holder: VH, position: Int) {
        vhs.add(holder)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        vhs.remove(holder)
    }

    override fun getItemCount(): Int = items.size

    fun toggleSelection(o:ITEM){

        val operatingHash = o.hashCode().toLong()

        if (isSingleSelection){

            if (selectedItemsHashes.keys.size > 0){
                for (item in items){
                    val itemHash = item.hashCode().toLong()

                    if (selectedItemsHashes.contains(itemHash)){
                        selectedItemsHashes.remove(itemHash)
                        //notifyItemChanged(items.indexOf(item))

                        updateCurrentViewHolders(o)
                    }
                }
            }

            if (selectedItemsHashes.contains(operatingHash)){
                selectedItemsHashes.remove(operatingHash)
            }else{
                selectedItemsHashes.put(operatingHash,items.indexOf(o))
            }

            updateCurrentViewHolders(o)
        }else{

            if (selectedItemsHashes.contains(operatingHash)){
                selectedItemsHashes.remove(operatingHash)
            }else{
                selectedItemsHashes.put(operatingHash,items.indexOf(o))
            }

            updateCurrentViewHolders(o)
        }

    }

    fun getSelection():HashMap<Long,Int>{
        return selectedItemsHashes
    }

    fun setSelection(newSelection:HashMap<Long,Int>){
        selectedItemsHashes = newSelection

        for (vh in vhs){
            notifyItemChanged(vh.adapterPosition)
        }
    }

    fun updateSelection(addSelection:HashMap<Long,Int>){

        for (e in addSelection){
            selectedItemsHashes.put(e.key,e.value)
        }

        for (vh in vhs){
            notifyItemChanged(vh.adapterPosition)
        }
    }

    fun isSelected(id:Long):Boolean{
        return selectedItemsHashes.contains(id)
    }

    fun getSelectedItems():ArrayList<ITEM>{

        val arr =  ArrayList<ITEM>()

            for (o in items){
                val pos = items.indexOf(o)

                if (isSelectable(pos) && isSelected(getItemId(pos)))arr.add(o)
            }

        return arr
    }

    fun setDataAndNotify(newData : ArrayList<ITEM>){

        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }

    fun appendDataAndNotify(newData: ArrayList<ITEM>){

        val notifyStart = items.size
        items.addAll(newData)
        notifyItemRangeInserted(notifyStart,newData.size)
    }

    fun selectInverse(){

        for (o in items){
            if (isSelectable(items.indexOf(o))){
            toggleSelection(o)
          }
        }

    }

    fun selectAll(){

        for (o in items){
            if (!isSelected(getItemId(items.indexOf(o))) && isSelectable(items.indexOf(o)))toggleSelection(o)
        }

    }

    fun clearSelection(){

        for (o in items){
            if (isSelected(getItemId(items.indexOf(o))) && isSelectable(items.indexOf(o)))toggleSelection(o)
        }

    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }
}
