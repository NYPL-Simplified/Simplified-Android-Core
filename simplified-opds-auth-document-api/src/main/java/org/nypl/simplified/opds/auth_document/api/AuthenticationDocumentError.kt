package org.nypl.simplified.opds.auth_document.api

import java.lang.Exception
import java.net.URI

/**
 * An error in a document.
 */

data class AuthenticationDocumentError(
  val source: URI,
  val message: String,
  val exception: Exception? = null)
