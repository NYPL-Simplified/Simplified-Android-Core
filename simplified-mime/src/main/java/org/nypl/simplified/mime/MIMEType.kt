package org.nypl.simplified.mime

import java.io.Serializable

/**
 * An RFC2045 MIME type.
 *
 * This type is serializable to allow passing values of this type between Android activities.
 * Absolutely no guaratees are made that serialized values will be readable by future versions
 * of the application.
 */

data class MIMEType(
  val type: String,
  val subtype: String,
  val parameters: Map<String, String>
) : Serializable {

  /**
   * The combined type and subtype (not including parameters)
   */

  val fullType: String = "${this.type}/${this.subtype}"

  override fun toString(): String {
    return this.fullType
  }
}
