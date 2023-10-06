package com.a.freeshare.fragment

import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    companion object{
        const val SELECTION_HASH_ARRAY = "selection_hash_array"
        const val ITEMS = "items"
        val TAG = BaseFragment::class.simpleName

        const val P2P_INFO = "p2p_info"
    }

    abstract fun hasCleared():Boolean

}