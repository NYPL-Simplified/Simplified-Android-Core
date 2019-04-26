package org.nypl.simplified.opds.core

import org.nypl.simplified.mime.MIMEType
import java.io.Serializable
import java.util.ArrayDeque
import java.util.Collections

/**
 * A linearized acquisition path. Represents a single path the application can take through an
 * acquisition process.
 *
 * This type is serializable to allow passing values of this type between Android activities.
 * Absolutely no guaratees are made that serialized values will be readable by future versions
 * of the application.
 */

data class OPDSAcquisitionPath(
  val next: OPDSAcquisition,
  val sequence: List<MIMEType>)
  : Serializable {

  /**
   * @return The final content type in the acquisition path
   */

  fun finalContentType(): MIMEType {
    return this.sequence.last()
  }

  companion object {

    /**
     * Linearize all direct and indirect acquisitions within the given acquisition.
     */

    fun linearizeAcquisitions(
      acquisitions: List<OPDSAcquisition>): List<OPDSAcquisitionPath> {
      return acquisitions.flatMap { acquisition -> linearizeAcquisition(acquisition) }
    }

    /**
     * Linearize all direct and indirect acquisitions within the given acquisition.
     */

    fun linearizeAcquisition(
      acquisition: OPDSAcquisition): List<OPDSAcquisitionPath> {

      val results = ArrayList<OPDSAcquisitionPath>()
      val typePath = ArrayList<MIMEType>()
      acquisition.type.map { mime -> typePath.add(mime) }

      if (acquisition.indirectAcquisitions.isEmpty()) {
        results.add(OPDSAcquisitionPath(
          next = acquisition,
          sequence = ArrayList(typePath)))
      } else {
        for (indirect in acquisition.indirectAcquisitions) {
          linearizeIndirect(acquisition, results, typePath, indirect)
        }
      }

      return Collections.unmodifiableList(results)
    }

    private fun linearizeIndirect(
      acquisition: OPDSAcquisition,
      results: ArrayList<OPDSAcquisitionPath>,
      typePath: ArrayList<MIMEType>,
      indirect: OPDSIndirectAcquisition) {
      typePath.add(indirect.type)

      if (indirect.indirectAcquisitions.isEmpty()) {
        results.add(OPDSAcquisitionPath(
          next = acquisition,
          sequence = ArrayList(typePath)))
      } else {
        for (subIndirect in indirect.indirectAcquisitions) {
          linearizeIndirect(acquisition, results, typePath, subIndirect)
        }
      }

      typePath.removeAt(typePath.size - 1)
    }
  }
}