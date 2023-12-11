package org.nypl.simplified.opds.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import java.net.URI

/**
 * The book is public domain.
 */

data class OPDSAvailabilityOpenAccess private constructor(

  /**
   * @return The revocation link, if any
   */

  val revoke: OptionType<URI>,
  private val endDate: OptionType<DateTime>
) : OPDSAvailabilityType {

  val endDateOrNull: DateTime?
    get() = this.endDate.getOrNull()

  val revokeOrNull: URI?
    get() = this.revoke.getOrNull()

  /**
   * Get availability end date (always none for OpenAccess)
   * @return end_date
   */

  override fun getEndDate(): OptionType<DateTime> {
    return this.endDate
  }

  override fun toString(): String {
    val sb = StringBuilder("OPDSAvailabilityOpenAccess{")
    sb.append("revoke=")
      .append(revoke)
    sb.append('}')
    return sb.toString()
  }

  override fun <A, E : Exception?> matchAvailability(
    m: OPDSAvailabilityMatcherType<A, E>
  ): A {
    return m.onOpenAccess(this)
  }

  companion object {
    private const val serialVersionUID = 1L

    /**
     * @param revoke The revocation link, if any
     *
     * @return An "open access" availability value
     */

    @JvmStatic
    @JvmOverloads
    operator fun get(
      revoke: OptionType<URI>,
      endDate: OptionType<DateTime> = Option.none(),
    ): OPDSAvailabilityOpenAccess {
      return OPDSAvailabilityOpenAccess(revoke, endDate)
    }
  }
}
