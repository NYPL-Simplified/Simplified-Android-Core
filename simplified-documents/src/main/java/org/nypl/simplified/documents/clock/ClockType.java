package org.nypl.simplified.documents.clock;

/**
 * Interface for fetching the current time.
 */

public interface ClockType
{
  /**
   * @return The current time in milliseconds since the epoch.
   */

  long clockNow();
}
