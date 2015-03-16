package org.nypl.simplified.app;

/**
 * Information about the device heap.
 */

public interface MemoryControllerType
{
  /**
   * @return The approximate size in megabytes that the application is allowed
   *         to use.
   */

  int memoryGetSize();

  /**
   * @return <code>true</code> if the application should assume that memory is
   *         in critically short supply. This is a one-time judgement based
   *         entirely on {@link #memoryGetSize()} and will not change for the
   *         lifetime of the application.
   */

  boolean memoryIsSmall();
}
