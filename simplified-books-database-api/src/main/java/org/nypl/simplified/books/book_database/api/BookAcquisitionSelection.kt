package org.nypl.simplified.books.book_database.api

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
import org.slf4j.LoggerFactory

/**
 * Functions to pick an acquisition based on the supported formats.
 */

object BookAcquisitionSelection {

  private val logger = LoggerFactory.getLogger(BookAcquisitionSelection::class.java)

  /**
   * Pick the preferred acquisition path from the list of acquisition paths. The selection is made
   * based on the supported relations of the acquisitions, and the available final content
   * types.
   *
   * @return A preferred acquisition, or nothing if none of the acquisitions were suitable
   */

  fun preferredAcquisition(acquisitions: List<OPDSAcquisitionPath>): OptionType<OPDSAcquisitionPath> {
    this.logger.debug("{} possible acquisition paths", acquisitions.size)

    val onlySupportedRelations: List<OPDSAcquisitionPath> =
      acquisitions.filter { acquisition -> relationIsSupported(acquisition.next) }

    this.logger.debug("{} supported relations", onlySupportedRelations.size)

    for (acquisitionPath in onlySupportedRelations) {
      this.logger.debug("checking path {}", acquisitionPath.show())

      /*
       * Check that all types in the sequence except the last are borrowable, and that
       * the last type in the sequence is usable as a book.
       */

      val typeSequence = acquisitionPath.sequence
      if (typeSequence.size == 1) {
        if (BookFormats.isSupportedBookMimeType(typeSequence.first())) {
          this.logger.debug("accepted path {} as directly borrowable", acquisitionPath.show())
          return Option.some(acquisitionPath)
        }
      }

      val head = typeSequence.dropLast(1)
      val tail = typeSequence.last()
      if (head.all(BookFormats::isSupportedBorrowMimeType)
        && BookFormats.isSupportedBookMimeType(tail)) {
        this.logger.debug("accepted path {} as all elements are supported", acquisitionPath.show())
        return Option.some(acquisitionPath)
      }
    }

    this.logger.debug("none of the given acquisition paths are usable")
    return Option.none()
  }

  private fun relationIsSupported(acquisition: OPDSAcquisition): Boolean {
    return when (acquisition.relation) {
      ACQUISITION_BORROW, ACQUISITION_GENERIC, ACQUISITION_OPEN_ACCESS -> true
      ACQUISITION_BUY, ACQUISITION_SAMPLE, ACQUISITION_SUBSCRIBE -> false
    }
  }
}
