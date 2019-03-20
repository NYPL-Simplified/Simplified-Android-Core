package org.nypl.simplified.opds.core

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.assertions.Assertions
import org.nypl.simplified.mime.MIMEType
import java.io.Serializable
import java.net.URI

/**
 * A specific OPDS acquisition.
 *
 * http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Acquisition_Feeds
 */

data class OPDSAcquisition(

  /**
   * The relation of the acquisition
   */

  val relation: OPDSAcquisitionRelation,

  /**
   * The URI of the acquisition
   */

  val uri: URI,

  /**
   * The MIME type of immediately retrievable content, if any.
   */

  val type: OptionType<MIMEType>,

  /**
   * The set of indirect acquisitions
   */

  val indirectAcquisitions: List<OPDSIndirectAcquisition>) : Serializable {

  init {
    if (this.type.isNone) {
      Assertions.checkPrecondition(
        !this.indirectAcquisitions.isEmpty(),
        "If no acquisition type is provided, a set of indirect acquisitions must be provided")
    }
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}
