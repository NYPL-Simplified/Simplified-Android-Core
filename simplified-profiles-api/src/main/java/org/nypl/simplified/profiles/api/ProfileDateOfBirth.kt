package org.nypl.simplified.profiles.api

import org.joda.time.DateTime
import org.joda.time.Years
import org.joda.time.format.ISODateTimeFormat

/**
 * A date-of-birth value.
 */

data class ProfileDateOfBirth(

  /**
   * The date the profile holder was born.
   */

  val date: DateTime,

  /**
   * `true` if the date of birth was synthesized for privacy reasons.
   */

  val isSynthesized: Boolean
) {

  /**
   * Show the date as an ISO string.
   */

  fun show(): String {
    return ISODateTimeFormat.dateTime().print(this.date)
  }

  /*
   * The age in years that date of birth implies.
   */

  fun yearsOld(now: DateTime): Int =
    Years.yearsBetween(this.date, now).years
}
