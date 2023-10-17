package com.a.freeshare.fragment.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.a.freeshare.R
import com.a.freeshare.fragment.BaseFragment

class SearchFragment : BaseFragment() {

    companion object{

        const val TAG = "SearchFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_search,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<EditText>(R.id.fragment_search_edittext).apply {

            addTextChangedListener(object :TextWatcher{

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(p0: Editable?) {

                    if (p0 != null){
                        requireActivity().supportFragmentManager.findFragmentByTag(SearchResultFragmentList.TAG)?.also {
                            (it as SearchResultFragmentList).filter(text.toString().trim())
                        }
                    }
                }
            })
        }

    }
    override fun hasCleared(): Boolean {
        return true
    }
}