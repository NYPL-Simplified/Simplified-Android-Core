package org.nypl.simplified.app;

/**
 * Unambiguous indication of network connectivity.
 */

public interface NetworkConnectivityType {
  /**
   * @return {@code true} iff network connectivity is available.
   */

  boolean isNetworkAvailable();

  /**
   * @return {@code true} iff WIFI network connectivity is available.
   */

  boolean isWifiAvailable();
}
