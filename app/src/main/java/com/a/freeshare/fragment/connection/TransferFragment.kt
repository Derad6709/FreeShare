package com.a.freeshare.fragment.connection

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.TransferImpl
import com.a.freeshare.obj.FileItem

class TransferFragment:BaseFragment(),TransferImpl<FileItem> {

    inner class NonPredictiveLinearLayoutManager(context:Context):LinearLayoutManager(context){
        override fun supportsPredictiveItemAnimations(): Boolean {
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


    override fun hasCleared(): Boolean {
        return true
    }

    override fun onStartSend(o: FileItem) {

    }

    override fun onBytesSent(o: FileItem, bytes: Long) {

    }

    override fun onEndSend(o: FileItem) {

    }

    override fun onStartReceive(o: FileItem) {

    }

    override fun onBytesReceived(o: FileItem) {

    }

    override fun onEndReceive(o: FileItem) {

    }
}