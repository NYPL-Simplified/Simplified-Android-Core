package org.librarysimplified.documents

import java.net.URL

/**
 * The base type of documents.
 */

interface DocumentType {

  /**
   * Try fetching the latest version of the document.
   */

  fun update()

  /**
   * The current URL of the document
   */

  val readableURL: URL
}
