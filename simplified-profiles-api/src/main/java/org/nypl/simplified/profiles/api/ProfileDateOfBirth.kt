package org.nypl.simplified.profiles.api

import org.joda.time.LocalDate
import org.joda.time.Years

/**
 * A date-of-birth value.
 */

data class ProfileDateOfBirth(

  /**
   * The date the profile holder was born.
   */

  val date: LocalDate,

  /**
   * `true` if the date of birth was synthesized for privacy reasons.
   */

  val isSynthesized: Boolean) {

  /*
   * The age in years that date of birth implies.
   */

  fun yearsOld(now: LocalDate): Int =
    Years.yearsBetween(this.date, now).years
}
