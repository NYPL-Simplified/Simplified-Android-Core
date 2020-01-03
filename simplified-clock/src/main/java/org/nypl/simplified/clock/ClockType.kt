package org.nypl.simplified.clock

import org.joda.time.Instant

/**
 * Interface for fetching the current time.
 */

interface ClockType {

  /**
   * @return The current time
   */

  fun clockNow(): Instant
}
