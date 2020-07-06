package org.nypl.simplified.networkconnectivity.api

/**
 * Unambiguous indication of network connectivity.
 */

interface NetworkConnectivityType {

  /**
   * @return `true` if network connectivity is available.
   */

  val isNetworkAvailable: Boolean

  /**
   * @return `true` if WIFI network connectivity is available.
   */

  val isWifiAvailable: Boolean
}
