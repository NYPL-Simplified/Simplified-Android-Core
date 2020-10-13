package org.nypl.simplified.books.borrowing.internal

import org.joda.time.Instant
import org.joda.time.Seconds

/**
 * A units per second counter.
 */

class BorrowUnitsPerSecond(clock: () -> Instant) {

  @Volatile
  private var first = true

  @Volatile
  private var upsCurrent = 0L

  @Volatile
  private var upsLast = 0L

  @Volatile
  private var timeLast = clock.invoke()

  @Volatile
  private var timeCurrent = clock.invoke()

  /**
   * The number of units updated over the previous second
   */

  val now: Long
    get() = this.upsLast

  /**
   * Update the counter, returning the number of units updated over the previous second.
   */

  fun update(x: Long): Boolean {
    this.upsCurrent += Math.max(0L, x)
    this.timeCurrent = Instant.now()
    if (Seconds.secondsBetween(this.timeLast, this.timeCurrent).seconds >= 1 || this.first) {
      this.first = false
      this.upsLast = this.upsCurrent
      this.upsCurrent = 0L
      this.timeLast = this.timeCurrent
      return true
    }
    return false
  }
}
