package org.nypl.simplified.opds.auth_document.api

import java.lang.Exception

data class AuthenticationDocumentError(
  val message: String,
  val exception: Exception? = null)
