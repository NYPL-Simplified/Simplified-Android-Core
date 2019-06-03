package org.nypl.simplified.opds.auth_document.api

import java.lang.Exception

data class AuthenticationDocumentWarning(
  val message: String,
  val exception: Exception?)
