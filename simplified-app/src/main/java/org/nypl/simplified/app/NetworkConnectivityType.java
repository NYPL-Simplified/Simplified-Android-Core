package org.nypl.simplified.app;

/**
 * Unambiguous indication of network connectivity.
 */

public interface NetworkConnectivityType
{
  /**
   * @return <tt>true</tt> iff network connectivity is available.
   */

  boolean isNetworkAvailable();
}
