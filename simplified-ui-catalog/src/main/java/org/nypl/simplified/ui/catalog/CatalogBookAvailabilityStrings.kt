package org.nypl.simplified.ui.catalog

import android.content.res.Resources
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.Hours
import org.nypl.simplified.books.book_registry.BookStatus
import java.util.concurrent.TimeUnit

/**
 * Functions that map book availability values to human-readable strings.
 */

object CatalogBookAvailabilityStrings {

  /**
   * Produce a human-readable string for the given book status.
   *
   * @param resources The application resources
   * @param status The status value
   *
   * @return A descriptive string
   */

  fun statusString(
    resources: Resources,
    status: BookStatus
  ): String {
    return when (status) {
      is BookStatus.Held.HeldInQueue ->
        onHeld(resources, Option.of(status.endDate), Option.of(status.queuePosition))
      is BookStatus.Held.HeldReady ->
        onLoanable(resources)
      is BookStatus.Holdable ->
        onHoldable(resources)
      is BookStatus.Loanable ->
        onLoanable(resources)
      is BookStatus.Loaned.LoanedNotDownloaded ->
        onLoaned(resources, Option.of(status.loanExpiryDate))
      is BookStatus.Loaned.LoanedDownloaded ->
        onLoaned(resources, Option.of(status.loanExpiryDate))
      is BookStatus.RequestingLoan ->
        ""
      is BookStatus.Revoked ->
        onRevoked(resources)
      is BookStatus.FailedLoan ->
        ""
      is BookStatus.FailedRevoke ->
        ""
      is BookStatus.FailedDownload ->
        ""
      is BookStatus.RequestingRevoke ->
        ""
      is BookStatus.RequestingDownload ->
        ""
      is BookStatus.Downloading ->
        ""
    }
  }

  private fun onRevoked(resources: Resources): String {
    return resources.getString(R.string.catalogBookAvailabilityRevoked)
  }

  private fun onOpenAccess(resources: Resources): String {
    return resources.getString(R.string.catalogBookAvailabilityOpenAccess)
  }

  private fun onLoanable(resources: Resources): String {
    return resources.getString(R.string.catalogBookAvailabilityLoanable)
  }

  private fun onHoldable(resources: Resources): String {
    return resources.getString(R.string.catalogBookAvailabilityHoldable)
  }

  private fun onLoaned(
    resources: Resources,
    expiryOpt: OptionType<DateTime>
  ): String {
    /*
     * If there is an expiry time, display it.
     */

    if (expiryOpt is Some<DateTime>) {
      val expiry = expiryOpt.get()
      val now = DateTime.now()
      return resources.getString(
        R.string.catalogBookAvailabilityLoanedTimed, this.intervalString(resources, now, expiry)
      )
    }

    /*
     * Otherwise, show an indefinite loan.
     */

    return resources.getString(R.string.catalogBookAvailabilityLoanedIndefinite)
  }

  private fun onReserved(
    resources: Resources,
    expiryOpt: OptionType<DateTime>
  ): String {
    /*
     * If there is an expiry time, display it.
     */

    if (expiryOpt is Some<DateTime>) {
      val expiry = expiryOpt.get()
      val now = DateTime.now()
      return resources.getString(
        R.string.catalogBookAvailabilityReservedTimed,
        this.intervalString(resources, now, expiry)
      )
    }

    /*
     * Otherwise, show an indefinite reservation.
     */

    return resources.getString(R.string.catalogBookAvailabilityReservedIndefinite)
  }

  private fun onHeld(
    resources: Resources,
    endDateOpt: OptionType<DateTime>,
    queuePositionOpt: OptionType<Int>
  ): String {
    /*
     * If there is an availability date, show this in preference to
     * anything else.
     */

    if (endDateOpt is Some<DateTime>) {
      val endDate = endDateOpt.get()
      val now = DateTime.now()
      return resources.getString(
        R.string.catalogBookAvailabilityHeldTimed,
        this.intervalString(resources, now, endDate)
      )
    }

    /*
     * If there is a queue position, attempt to show this instead.
     */

    if (queuePositionOpt is Some<Int>) {
      return resources.getString(R.string.catalogBookAvailabilityHeldQueue, queuePositionOpt.get())
    }

    /**
     * Otherwise, show an indefinite hold.
     */

    return resources.getString(R.string.catalogBookAvailabilityHeldIndefinite)
  }

  /**
   *
   * Construct a time interval string based on the given times. The string
   * will be a localized form of:
   *
   *  * `less than an hour` iff the period is under one hour
   *  * `n hours` iff the period is under one day
   *  * `n days` otherwise
   *
   * @param resources The application resources
   * @param lower The lower bound of the time period
   * @param upper The upper bound of the time period
   *
   * @return A time interval string
   */

  fun intervalString(
    resources: Resources,
    lower: DateTime,
    upper: DateTime
  ): String {
    val hours = this.calendarHoursBetween(lower, upper)

    if (hours < 1) {
      return resources.getString(R.string.catalogBookIntervalSubHour)
    }
    if (hours < 24) {
      val base = resources.getString(R.string.catalogBookIntervalHours)
      return String.format("%d %s", hours, base)
    }

    val base = resources.getString(R.string.catalogBookIntervalDays)
    return String.format("%d %s", TimeUnit.HOURS.toDays(hours), base)
  }

  /**
   * Construct a short time interval string like "3w", with units up to a year.
   *
   * @param resources The application resources
   * @param lower The lower bound of the time period
   * @param upper The upper bound of the time period
   *
   * @return A time interval string
   */

  fun intervalStringShort(
    resources: Resources,
    lower: DateTime,
    upper: DateTime
  ): String {
    val hours = this.calendarHoursBetween(lower, upper)
    val days = TimeUnit.HOURS.toDays(hours)
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    val unit: String
    val value: Long

    if (years > 0) {
      unit = resources.getString(R.string.catalogBookIntervalYearsShort)
      value = years
    } else if (weeks > 8) {
      unit = resources.getString(R.string.catalogBookIntervalMonthsShort)
      value = months
    } else if (weeks > 0) {
      unit = resources.getString(R.string.catalogBookIntervalWeeksShort)
      value = weeks
    } else if (days > 0) {
      unit = resources.getString(R.string.catalogBookIntervalDaysShort)
      value = days
    } else {
      unit = resources.getString(R.string.catalogBookIntervalHoursShort)
      value = hours
    }

    return String.format("%d%s", value, unit)
  }

  private fun calendarHoursBetween(
    start: DateTime,
    end: DateTime
  ): Long {
    return Hours.hoursBetween(start, end).hours.toLong()
  }
}
