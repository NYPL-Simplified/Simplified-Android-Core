package org.nypl.simplified.profiles.api

import java.util.SortedMap

/**
 * The attributes for a profile.
 *
 * An _attribute_ is distinct from a _preference_ in that the application is NOT permitted to
 * behave differently based upon the value of a _attribute_. Attributes are typically only useful
 * for analytics data and display-only touches (such as displaying a female head icon in a profile
 * selection screen if the gender attribute is set to female). Attributes are not typed.
 *
 * @see ProfileAttributes
 */

data class ProfileAttributes(
  val attributes: SortedMap<String, String>
) {

  val grade: String? =
    this.attributes[GRADE_ATTRIBUTE_KEY]

  val school: String? =
    this.attributes[SCHOOL_ATTRIBUTE_KEY]

  val role: String? =
    this.attributes[ROLE_ATTRIBUTE_KEY]

  val gender: String? =
    this.attributes[GENDER_ATTRIBUTE_KEY]

  companion object {
    const val GRADE_ATTRIBUTE_KEY = "grade"
    const val SCHOOL_ATTRIBUTE_KEY = "school"
    const val GENDER_ATTRIBUTE_KEY = "gender"
    const val ROLE_ATTRIBUTE_KEY = "role"
  }
}
