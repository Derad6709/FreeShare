package com.a.freeshare.obj

data class HelperItem(var name:String?,var absPath:String?,var mime:String?,var maxValue:Long?,var itemState: ItemState,var sharedType:Int,var currentValue:Long = 0){

    constructor(itemState: ItemState,sharedType: Int):this(null,null,null,null,itemState, sharedType)

    enum class ItemState{
        STARTED,
        IN_PROGRESS,
        ENDED,
        ENQUEUED,
        SKIPPED
    }

    companion object{
        const val SENT = 1
        const val RECEIVED = 2
    }
}