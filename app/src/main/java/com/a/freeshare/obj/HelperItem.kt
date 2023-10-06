package com.a.freeshare.obj

data class HelperItem(val name:String,val absPath:String,val mime:String?,val maxValue:Long,var itemState: ItemState,var sharedType:Int,var currentValue:Long = 0){

    enum class ItemState{
        STARTED,
        IN_PROGRESS,
        ENDED
    }

    companion object{
        const val SENT = 1
        const val RECEIVED = 2
    }
}