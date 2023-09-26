package com.a.freeshare.fragment

import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    companion object{
        const val SELECTION_HASH_ARRAY = "selection_hash_array"
        val TAG = BaseFragment::class.simpleName
        const val ITEMS = "items"
    }

    abstract fun hasCleared():Boolean

}