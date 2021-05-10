package org.nypl.simplified.ui.catalog.saml20

import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class CatalogSAML20Event {

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : CatalogSAML20Event()

  object LoginSucceeded : CatalogSAML20Event()
}
