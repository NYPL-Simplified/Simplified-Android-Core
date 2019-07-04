package org.nypl.simplified.app

import android.content.Context
import android.net.ConnectivityManager

internal class SimplifiedNetworkConnectivity(
  private val context: Context) : NetworkConnectivityType {

  override fun isNetworkAvailable(): Boolean {
    val service = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val info = service.activeNetworkInfo ?: return false
    return info.isConnectedOrConnecting
  }

  override fun isWifiAvailable(): Boolean {
    val service = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val info = service.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
    return info.isConnectedOrConnecting
  }
}
