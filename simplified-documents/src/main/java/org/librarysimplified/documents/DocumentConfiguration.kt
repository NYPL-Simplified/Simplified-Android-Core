package org.librarysimplified.documents

import java.net.URI

/**
 * The configuration for a single document.
 */

data class DocumentConfiguration(

  /**
   * The name of the document file.
   */

  val name: String,

  /**
   * The remote URI used to fetch new versions of the document.
   */

  val remoteURI: URI
)
