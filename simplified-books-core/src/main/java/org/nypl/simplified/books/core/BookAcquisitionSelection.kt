package org.nypl.simplified.books.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE

/**
 * Functions to pick an acquisition based on the supported formats.
 */

object BookAcquisitionSelection {

  /**
   * Pick the preferred acquisition from the list of acquisitions. The selection is made
   * based on the supported relations of the acquisitions, and the available final content
   * types.
   *
   * @return A preferred acquisition, or nothing if none of the acquisitions were suitable
   */

  fun preferredAcquisition(acquisitions: List<OPDSAcquisition>): OptionType<OPDSAcquisition> {

    val onlySupportedRelations: List<OPDSAcquisition> =
      acquisitions.filter { acquisition -> relationIsSupported(acquisition) }

    for (acquisition in onlySupportedRelations) {
      val supportedContentTypes = BookFormats.supportedBookMimeTypes()
      val availableContentTypes = acquisition.availableFinalContentTypeNames()
      for (contentType in supportedContentTypes) {
        if (availableContentTypes.contains(contentType)) {
          return Option.some(acquisition)
        }
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
