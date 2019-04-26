package org.nypl.simplified.opds.core

import org.nypl.simplified.mime.MIMEType
import java.io.Serializable

/**
 * A tree of indirect acquisitions.
 */

data class OPDSIndirectAcquisition(

  /**
   * The MIME type of the indirectly obtainable content.
   */

  val type: MIMEType,

  /**
   * Zero or more nested indirect acquisitions.
   */

  val indirectAcquisitions: List<OPDSIndirectAcquisition>) : Serializable
