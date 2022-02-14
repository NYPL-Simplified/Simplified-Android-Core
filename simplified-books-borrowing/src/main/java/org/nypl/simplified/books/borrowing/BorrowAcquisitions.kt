package org.nypl.simplified.books.borrowing

import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPaths

/**
 * Functions to select book acquisitions based on the available format support.
 */

object BorrowAcquisitions {

  /**
   * List of DRMs that should preferably be used, in order.
   */

  private val preferredDRMs: List<BookDRMKind> =
    listOf(
      BookDRMKind.ACS,
      BookDRMKind.AXIS
    )

  /**
   * Pick the preferred acquisition for the OPDS feed entry.
   */

  fun pickBestAcquisitionPath(
    support: BookFormatSupportType,
    entry: OPDSAcquisitionFeedEntry
  ): OPDSAcquisitionPath? {
    val paths = OPDSAcquisitionPaths.linearize(entry)
    val filtered = paths.filter { support.isSupportedPath(it.asMIMETypes()) }
      .sortedByDescending { acquisitionPathPriority(support, it) }
    return filtered.firstOrNull()
  }

  private fun acquisitionPathPriority(
    support: BookFormatSupportType,
    path: OPDSAcquisitionPath
  ): Int {
    val drmKind = support.getDRMKind(path.asMIMETypes())
    val drmIndex = preferredDRMs.indexOf(drmKind)
    return if (drmIndex == -1) 0 else preferredDRMs.size - drmIndex
  }
}
