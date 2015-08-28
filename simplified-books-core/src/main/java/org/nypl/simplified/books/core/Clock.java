package org.nypl.simplified.books.core;

/**
 * Default implementation of the {@link ClockType} interface.
 */

public final class Clock implements ClockType
{
  private Clock()
  {

  }

  /**
   * @return A reference to the default clock.
   */

  public static ClockType get()
  {
    return new Clock();
  }

  @Override public long clockNow()
  {
    return System.currentTimeMillis();
  }
}
