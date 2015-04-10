package org.nypl.simplified.app;

/**
 * The interface that controls the initial syncing and loading of books.
 */

public interface SimplifiedAppInitialSyncType
{
  /**
   * Load and synchronize books once, ignoring errors and ignoring the fact
   * that the user may not be logged in.
   */

  void syncInitial();
}
