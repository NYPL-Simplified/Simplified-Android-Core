package org.nypl.simplified.opds.core

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Unit
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.net.URI

/**
 * The book is held and is ready to be checked out now.
 */

data class OPDSAvailabilityHeldReady private constructor(
  val endDateValue: OptionType<DateTime>,
  val revoke: OptionType<URI>
) : OPDSAvailabilityType {

  val endDateOrNull: DateTime?
    get() = this.endDateValue.getOrNull()

  val revokeOrNull: URI?
    get() = this.revoke.getOrNull()

  override fun getEndDate(): OptionType<DateTime> {
    return this.endDateValue
  }

  override fun <A, E : Exception> matchAvailability(
    m: OPDSAvailabilityMatcherType<A, E>
  ): A {
    return m.onHeldReady(this)
  }

  override fun toString(): String {
    val fmt = ISODateTimeFormat.dateTime()
    val b = StringBuilder(128)
    b.append("[OPDSAvailabilityHeldReady end_date=")
    this.endDate.map { e: DateTime? ->
      b.append(fmt.print(e))
      Unit.unit()
    }
    b.append(" revoke=")
    b.append(this.revoke)
    b.append("]")
    return b.toString()
  }

  companion object {
    private const val serialVersionUID = 1L

    /**
     * @param endDate The end date (if known)
     * @param revoke The reservation revocation link, if any
     *
     * @return A value that states that a book is on hold
     */

    @JvmStatic
    fun get(
      endDate: OptionType<DateTime>,
      revoke: OptionType<URI>
    ): OPDSAvailabilityHeldReady {
      return OPDSAvailabilityHeldReady(
        endDateValue = endDate,
        revoke = revoke
      )
    }
  }
}
