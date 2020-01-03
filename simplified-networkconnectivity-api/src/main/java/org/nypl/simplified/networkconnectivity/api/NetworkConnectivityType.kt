package org.nypl.simplified.networkconnectivity.api

/**
 * Unambiguous indication of network connectivity.
 */

interface NetworkConnectivityType {

  /**
   * @return `true` iff network connectivity is available.
   */

  val isNetworkAvailable: Boolean

  /**
   * @return `true` iff WIFI network connectivity is available.
   */

  val isWifiAvailable: Boolean

}
