package org.nypl.simplified.ui.catalog.saml20

import org.nypl.simplified.books.api.BookID
import java.io.Serializable
import java.net.URI

/**
 * Parameters for the book download login fragment.
 */

data class CatalogSAML20FragmentParameters(
  val bookID: BookID,
  val downloadURI: URI
) : Serializable
