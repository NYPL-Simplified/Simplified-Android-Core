package org.nypl.simplified.books.book_database.api

import net.jcip.annotations.ThreadSafe
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.books.api.BookDRMInformation
import java.io.File
import java.io.IOException

/**
 * The `BookDRMInformationHandle` class represents a handle to the DRM information for
 * a given book. It is used to read and write from/to the database.
 */

@ThreadSafe
sealed class BookDRMInformationHandle {

  /**
   * @return The most recent format information
   */

  abstract val info: BookDRMInformation

  /**
   * The handle used to read/write ACS information.
   */

  abstract class ACSHandle : BookDRMInformationHandle() {
    abstract override val info: BookDRMInformation.ACS

    /**
     * Set the ACSM file for the book.
     *
     * @param ascm The ACSM file, if any
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun setACSMFile(
      acsm: File?
    ): BookDRMInformation.ACS

    /**
     * Set the Adobe rights information for the book.
     *
     * @param loan The loan
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun setAdobeRightsInformation(
      loan: AdobeAdeptLoan?
    ): BookDRMInformation.ACS
  }

  /**
   * The handle used to read/write LCP information.
   */

  abstract class LCPHandle : BookDRMInformationHandle() {
    abstract override val info: BookDRMInformation.LCP
  }

  /**
   * The handle that represents no DRM.
   */

  abstract class NoneHandle : BookDRMInformationHandle() {
    abstract override val info: BookDRMInformation.None
  }
}
