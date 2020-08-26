package org.nypl.simplified.books.api

import org.nypl.drm.core.AdobeAdeptLoan
import java.io.File

/**
 * The `BookDRMInformation` class represents an immutable snapshot of the current DRM
 * information associated with a book.
 */

sealed class BookDRMInformation {

  /**
   * The kind of DRM
   */

  abstract val kind: BookDRMKind

  /**
   * The Adobe ACS information associated with a book.
   */

  data class ACS(

    /**
     * The ACSM file. This is only present if an attempt has been made to fulfill the book.
     */

    val acsmFile: File?,

    /**
     * The rights information. This is only present if the book has been fulfilled.
     */

    val rights: Pair<File, AdobeAdeptLoan>?
  ) : BookDRMInformation() {
    override val kind: BookDRMKind = BookDRMKind.ACS
  }

  /**
   * The LCP information associated with a book.
   */

  data class LCP(

    /**
     * LCP currently has no associated information, so an unused `Unit` typed field is
     * added here to avoid having to refactor from an `object` to a `data class` later.
     */

    private val unused: Unit = Unit
  ) : BookDRMInformation() {
    override val kind: BookDRMKind = BookDRMKind.LCP
  }

  /**
   * The book either has no DRM, or uses some kind of external DRM system that the book database
   * doesn't know about (such as proprietary AudioBook DRM).
   */

  object None : BookDRMInformation() {
    override val kind: BookDRMKind = BookDRMKind.NONE
  }
}
