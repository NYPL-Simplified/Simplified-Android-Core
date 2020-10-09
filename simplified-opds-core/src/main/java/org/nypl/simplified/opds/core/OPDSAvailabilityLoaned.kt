package org.nypl.simplified.opds.core

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.net.URI

/**
 * The book is loaned out to the user.
 */

data class OPDSAvailabilityLoaned private constructor(

  /**
   * @return The start date for the loan, if any
   */

  val startDate: OptionType<DateTime>,

  /**
   * @return A URI for revoking the hold, if any
   */

  val revoke: OptionType<URI>,

  private val endDate: OptionType<DateTime>
) : OPDSAvailabilityType {

  val startDateOrNull: DateTime?
    get() = this.startDate.getOrNull()

  val endDateOrNull: DateTime?
    get() = this.endDate.getOrNull()

  val revokeOrNull: URI?
    get() = this.revoke.getOrNull()

  /**
   * @return The end date for the loan, if any
   */

  override fun getEndDate(): OptionType<DateTime> {
    return this.endDate
  }

  override fun <A, E : Exception?> matchAvailability(
    m: OPDSAvailabilityMatcherType<A, E>
  ): A {
    return m.onLoaned(this)
  }

  override fun toString(): String {
    val fmt = ISODateTimeFormat.dateTime()
    val b = StringBuilder(128)
    b.append("[OPDSAvailabilityLoaned end_date=")
    b.append(this.endDate.map { c: DateTime? -> fmt.print(c) })
    b.append(" start_date=")
    b.append(this.startDate.map { c: DateTime? -> fmt.print(c) })
    b.append(" revoke=")
    b.append(this.revoke)
    b.append("]")
    return b.toString()
  }

  companion object {
    private const val serialVersionUID = 1L

    /**
     * @param startDate The start date for the loan
     * @param endDate The end date for the loan
     * @param revoke The optional revocation link for the loan
     *
     * @return An availability value that states that the given book is loaned
     */

    @JvmStatic
    operator fun get(
      startDate: OptionType<DateTime>,
      endDate: OptionType<DateTime>,
      revoke: OptionType<URI>
    ): OPDSAvailabilityLoaned {
      return OPDSAvailabilityLoaned(
        startDate = startDate,
        revoke = revoke,
        endDate = endDate
      )
    }
  }
}
