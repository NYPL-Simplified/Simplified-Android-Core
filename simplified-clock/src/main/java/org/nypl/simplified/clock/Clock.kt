package org.nypl.simplified.clock

import org.joda.time.Instant

/**
 * Default implementation of the [ClockType] interface.
 */

object Clock : ClockType {
  override fun clockNow(): Instant {
    return Instant.now()
  }
}
