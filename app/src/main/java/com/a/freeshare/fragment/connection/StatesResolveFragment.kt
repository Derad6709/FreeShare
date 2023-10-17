package com.a.freeshare.fragment.connection

import android.app.Dialog
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.a.freeshare.LocationBroadcastReceiver
import com.a.freeshare.R
import com.a.freeshare.WifiBroadcastReceiver
import com.a.freeshare.activity.SessionActivity
import com.a.freeshare.fragment.BaseFragment
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.impl.OnStateChangedListener
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.WirelessStateWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class StatesResolveFragment :DialogFragment(){

    companion object{
        const val  TAG:String = "StateResolveFragment"
    }

    private lateinit var wirelessStateWrapper: WirelessStateWrapper
    private lateinit var wifiReceiver:WifiBroadcastReceiver
    private lateinit var locationBroadcastReceiver: LocationBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setView(R.layout.fragment_states_resolve)

        val dialog = builder.create()

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStart() {
        super.onStart()

        isCancelable = false
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT)

        dialog?.findViewById<FrameLayout>(R.id.frameLayout1)?.visibility = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) View.GONE else View.VISIBLE

        wirelessStateWrapper = WirelessStateWrapper(requireActivity())

        wifiReceiver = WifiBroadcastReceiver(object : OnStateChangedListener {

            override fun onStateChanged(state: Int) {

                val localState = wirelessStateWrapper.getWifiState() == WifiManager.WIFI_STATE_ENABLED

                dialog?.findViewById<MaterialSwitch>(R.id.switch1)?.apply {

                     isChecked = localState
                }

                dialog?.findViewById<Button>(R.id.button2)?.isEnabled = wirelessStateWrapper.getWifiState() == WifiManager.WIFI_STATE_ENABLED && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    true
                }else{
                    wirelessStateWrapper.getLocationState()
                }


                dialog?.findViewById<ImageView>(R.id.imageView2)?.apply {

                    isEnabled = localState
                }
            }
        })

        locationBroadcastReceiver = LocationBroadcastReceiver(object :OnStateChangedListener{

            override fun onStateChanged(state: Int) {

                val localState = wirelessStateWrapper.getLocationState()

                dialog?.findViewById<MaterialSwitch>(R.id.switch2)?.apply {

                    isChecked = localState
                }

                dialog?.findViewById<Button>(R.id.button2)?.isEnabled = wirelessStateWrapper.getWifiState() == WifiManager.WIFI_STATE_ENABLED && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    true
                }else{
                    wirelessStateWrapper.getLocationState()
                }

                dialog?.findViewById<ImageView>(R.id.imageView3)?.apply {

                   isEnabled = localState
                }
            }
        })

        dialog?.findViewById<MaterialSwitch>(R.id.switch1)?.apply {

            isChecked = wirelessStateWrapper.getWifiState() == WifiManager.WIFI_STATE_ENABLED

            setOnCheckedChangeListener {b,checked ->

            if(wirelessStateWrapper.getWifiState() != WifiManager.WIFI_STATE_ENABLED && checked) {
                isChecked = false
                wirelessStateWrapper.tryEnableWifi()
            }else if (wirelessStateWrapper.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
                isChecked = true
             }
            }
        }

        dialog?.findViewById<MaterialSwitch>(R.id.switch2)?.apply {

            isChecked = wirelessStateWrapper.getLocationState()

            setOnCheckedChangeListener {b,checked ->

                if(!wirelessStateWrapper.getLocationState() && checked ) {
                    isChecked = false
                    wirelessStateWrapper.tryEnableLocation()
                }else if (wirelessStateWrapper.getLocationState()){
                    isChecked = true
                }
            }
        }

        dialog?.findViewById<Button>(R.id.button2)?.apply {


            setOnClickListener {

                requireActivity().unregisterReceiver(wifiReceiver)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    requireActivity().unregisterReceiver(locationBroadcastReceiver)
                }
                dismiss()

                if(requireActivity() is SessionActivity){
                    (requireActivity() as SessionActivity).init()
                }
            }
        }

        dialog?.findViewById<Button>(R.id.button3)?.apply {

            setOnClickListener {
                dismiss()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        requireActivity().registerReceiver(wifiReceiver,IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        })

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        requireActivity().registerReceiver(locationBroadcastReceiver,IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        })

    }
}