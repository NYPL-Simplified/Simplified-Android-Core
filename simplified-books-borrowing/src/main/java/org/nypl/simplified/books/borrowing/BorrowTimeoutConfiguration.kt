package org.nypl.simplified.books.borrowing

import java.util.concurrent.TimeUnit

/**
 * A specification of a timeout value.
 */

data class BorrowTimeoutConfiguration(
  val time: Long,
  val timeUnit: TimeUnit
)
