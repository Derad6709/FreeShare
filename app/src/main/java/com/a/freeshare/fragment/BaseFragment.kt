package com.a.freeshare.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a.freeshare.R
import com.a.freeshare.impl.CommonSelectionImpl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec

abstract class BaseFragment : Fragment() {

    companion object{
        const val SELECTION_HASH_MAP = "selection_hash"
        const val ITEMS = "items"
        const val LAYOUT_TYPE = "layout_type"
        val TAG = BaseFragment::class.simpleName

        const val SCROLL_POSITION ="scroll_pos"

        const val P2P_INFO = "p2p_info"
    }

    abstract fun hasCleared():Boolean

}