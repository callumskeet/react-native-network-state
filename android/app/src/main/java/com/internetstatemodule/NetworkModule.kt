package com.internetstatemodule

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule

@ReactModule(name = NetworkModule.REACT_CLASS)
class NetworkModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return REACT_CLASS
    }

    private fun getConnectivityManager(): ConnectivityManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            reactContext.applicationContext.getSystemService(ConnectivityManager::class.java)
        } else {
            reactContext.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
    }

    private fun getNetworkCapabilities(): NetworkCapabilities? {
        val connectivityManager = getConnectivityManager()
        val currentNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork
        } else {
            Log.e(TAG, connectivityManager.allNetworks.toString())
            connectivityManager.allNetworks[0]
        }
        return connectivityManager.getNetworkCapabilities(currentNetwork)
    }

    private fun getNetworkStateMap(caps: NetworkCapabilities?): WritableMap {
        val isInternetCapable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } else {
            isInternetCapable
        }
        val isNetworkSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED) == false
        } else {
            false
        }
        val isNetworkRestricted = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) == false
        val isVPN = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) == false

        val result = Arguments.createMap()
        result.putBoolean("isValidated", isValidated)
        result.putBoolean("isVPN", isVPN)
        result.putBoolean("isInternetCapable", isInternetCapable)
        result.putBoolean("isNetworkSuspended", isNetworkSuspended)
        result.putBoolean("isNetworkRestricted", isNetworkRestricted)
        return result
    }

    private fun getNetworkTransportMap(caps: NetworkCapabilities?): WritableMap {
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isBluetooth = caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isVPN = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val isWifiAware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) == true
        } else {
            false
        }
        val isUSB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_USB) == true
        } else {
            false
        }
        val isLoWPAN = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) == true
        } else {
            false
        }

        val result = Arguments.createMap()
        result.putBoolean("isCellular", isCellular)
        result.putBoolean("isBluetooth", isBluetooth)
        result.putBoolean("isWifi", isWifi)
        result.putBoolean("isVPN", isVPN)
        result.putBoolean("isEthernet", isEthernet)
        result.putBoolean("isWifiAware", isWifiAware)
        result.putBoolean("isUSB", isUSB)
        result.putBoolean("isLoWPAN", isLoWPAN)
        return result
    }

    private fun sendEvent(reactContext: ReactContext, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(NETWORK_LISTENER_EVENT, params)
    }

    @ReactMethod
    fun getNetworkState(promise: Promise) {
        try {
            val caps = getNetworkCapabilities()
            val result = getNetworkStateMap(caps)
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("ERR_NETWORK_NO_ACCESS_NETWORKINFO", e.toString(), e)
        }
    }

    @ReactMethod
    fun getNetworkTransport(promise: Promise) {
        try {
            val caps = getNetworkCapabilities()
            val result = getNetworkTransportMap(caps)
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("ERR_NETWORK_NO_ACCESS_NETWORKINFO", e.toString(), e)
        }
    }

    private val defaultNetworkCallback = object: ConnectivityManager.NetworkCallback() {
        fun updateAndSend(capabilities: NetworkCapabilities? = null) {
            val stateMap = getNetworkStateMap(capabilities)
            val transportMap = getNetworkTransportMap(capabilities)
            val event = Arguments.createMap()
            event.putMap("state", stateMap)
            event.putMap("transports", transportMap)
            sendEvent(reactContext, event)
        }

        override fun onLost(network : Network) {
            updateAndSend()
        }

        override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
            updateAndSend(networkCapabilities)
        }

        override fun onUnavailable() {
            updateAndSend()
        }
    }

    private lateinit var receiver: NetworkReceiver

    inner class NetworkReceiver : BroadcastReceiver() {
        private fun createStateMap(isConnected: Boolean?): WritableMap {
            val result = Arguments.createMap()
            result.putBoolean("isValidated", isConnected == true)
            result.putNull("isVPN")
            result.putNull("isInternetCapable")
            result.putNull("isNetworkSuspended")
            result.putNull("isNetworkRestricted")
            return result
        }

        override fun onReceive(context: Context, intent: Intent) {
            val outerThis = this@NetworkModule
            val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = conn.activeNetworkInfo
            val stateMap = createStateMap(networkInfo?.isConnected())
            sendEvent(outerThis.reactContext, stateMap)
        }
    }

    private fun addNetworkListener() {
        val connectivityManager = getConnectivityManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        } else {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            receiver = NetworkReceiver()
            reactContext.registerReceiver(receiver, filter)
        }
    }

    private fun removeNetworkListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager = getConnectivityManager()
            connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        } else {
            reactContext.unregisterReceiver(receiver)
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        addNetworkListener()
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        removeNetworkListener()
    }

    companion object {
        const val REACT_CLASS = "NetworkModule"
        const val NETWORK_LISTENER_EVENT = "$REACT_CLASS.NetworkListenerEvent"
    }
}
