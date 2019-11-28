package org.nypl.simplified.networkconnectivity

import android.content.Context
import android.net.ConnectivityManager
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType

/**
 * The default network connectivity implementation.
 */

class NetworkConnectivity private constructor(
  private val context: Context
) : NetworkConnectivityType {

  override val isNetworkAvailable: Boolean
    get() = run {
      val service = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val info = service.activeNetworkInfo ?: return false
      return info.isConnectedOrConnecting
    }

  override val isWifiAvailable: Boolean
    get() = run {
      val service = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val info = service.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
      return info.isConnectedOrConnecting
    }

  companion object {

    /**
     * Create a new network connectivity interface.
     */

    fun create(context: Context): NetworkConnectivityType {
      return NetworkConnectivity(context)
    }
  }

}