package org.nypl.simplified.opds.auth_document.api

import java.lang.Exception
import java.net.URI

/**
 * A warning in a document.
 */

data class AuthenticationDocumentWarning(
  val source: URI,
  val message: String,
  val exception: Exception?)
