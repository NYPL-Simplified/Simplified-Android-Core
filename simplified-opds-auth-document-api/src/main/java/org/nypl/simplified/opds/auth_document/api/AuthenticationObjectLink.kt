package org.nypl.simplified.opds.auth_document.api

import org.nypl.simplified.mime.MIMEType
import java.net.URI

/**
 * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#232-links"
 */

data class AuthenticationObjectLink(

  /**
   * URI or URI template of the linked resource
   */

  val href: URI,

  /**
   * Indicates that href is a URI template
   */

  val templated: Boolean = false,

  /**
   * Media type of the linked resource
   */

  val type: MIMEType? = null,

  /**
   * Title of the linked resource
   */

  val title: String? = null,

  /**
   * Relation between the resource and its containing collection
   */

  val rel: String? = null,

  /**
   * Height of the linked resource in pixels
   */

  val height: Int? = null,

  /**
   * Width of the linked resource in pixels
   */

  val width: Int? = null,

  /**
   * Duration of the linked resource in seconds
   */

  val duration: Double? = null,

  /**
   * Bit rate of the linked resource in kilobits per second
   */

  val bitrate: Double? = null)
