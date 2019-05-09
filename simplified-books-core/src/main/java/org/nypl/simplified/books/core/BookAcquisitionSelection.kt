package org.nypl.simplified.books.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation.ACQUISITION_SUBSCRIBE

/**
 * Functions to pick an acquisition based on the supported formats.
 */

object BookAcquisitionSelection {

  /**
   * Pick the preferred acquisition path from the list of acquisition paths. The selection is made
   * based on the supported relations of the acquisitions, and the available final content
   * types.
   *
   * @return A preferred acquisition, or nothing if none of the acquisitions were suitable
   */

  fun preferredAcquisition(acquisitions: List<OPDSAcquisitionPath>): OptionType<OPDSAcquisitionPath> {

    val onlySupportedRelations: List<OPDSAcquisitionPath> =
      acquisitions.filter { acquisition -> relationIsSupported(acquisition.next) }

    for (acquisitionPath in onlySupportedRelations) {

      /*
       * Check that all types in the sequence except the last are borrowable, and that
       * the last type in the sequence is usable as a book.
       */

      val typeSequence = acquisitionPath.sequence
      if (typeSequence.size == 1) {
        if (BookFormats.isSupportedBookMimeType(typeSequence.first())) {
          return Option.some(acquisitionPath)
        }
      }

      val head = typeSequence.dropLast(1)
      val tail = typeSequence.last()
      if (head.all(BookFormats.Companion::isSupportedBorrowMimeType)
        && BookFormats.isSupportedBookMimeType(tail)) {
        return Option.some(acquisitionPath)
      }
    }

    return Option.none()
  }

  private fun relationIsSupported(acquisition: OPDSAcquisition): Boolean {
    return when (acquisition.relation) {
      ACQUISITION_BORROW, ACQUISITION_GENERIC, ACQUISITION_OPEN_ACCESS -> true
      ACQUISITION_BUY, ACQUISITION_SAMPLE, ACQUISITION_SUBSCRIBE -> false
    }
  }
}
