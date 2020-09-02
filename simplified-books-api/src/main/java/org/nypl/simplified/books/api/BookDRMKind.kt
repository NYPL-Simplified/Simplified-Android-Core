package org.nypl.simplified.books.api

/**
 * The `BookDRMKind` class identifies a type of DRM.
 */

enum class BookDRMKind {

  /**
   * See [BookDRMInformation.None]
   */

  NONE,

  /**
   * See [BookDRMInformation.LCP]
   */

  LCP,

  /**
   * See [BookDRMInformation.ACS]
   */

  ACS
}
