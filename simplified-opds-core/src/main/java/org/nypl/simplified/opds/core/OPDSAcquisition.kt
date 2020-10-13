package org.nypl.simplified.opds.core

import one.irradia.mime.api.MIMEType
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

  val relation: Relation,

  /**
   * The URI of the acquisition
   */

  val uri: URI,

  /**
   * The MIME type of immediately retrievable content, if any.
   */

  val type: MIMEType,

  /**
   * The set of indirect acquisitions
   */

  val indirectAcquisitions: List<OPDSIndirectAcquisition>
) : Serializable {

  /**
   * @return The set of final content types. That is, the set of content types that are accessible
   * if all acquisition paths are followed to their conclusions
   */

  fun availableFinalContentTypes(): Set<MIMEType> {
    val set = mutableSetOf<MIMEType>()
    val paths = OPDSAcquisitionPaths.linearize(this)
    for (path in paths) {
      set.add(path.elements.last().mimeType)
    }
    return set
  }

  /**
   * The specific type of acquisition.
   */

  enum class Relation(val uri: URI) {

    /**
     * An item can be borrowed.
     */

    ACQUISITION_BORROW(URI.create("http://opds-spec.org/acquisition/borrow")),

    /**
     * An item can be bought.
     */

    ACQUISITION_BUY(URI.create("http://opds-spec.org/acquisition/buy")),

    /**
     * An item can be obtained.
     */

    ACQUISITION_GENERIC(URI.create("http://opds-spec.org/acquisition")),

    /**
     * An item is open access (possibly public domain).
     */

    ACQUISITION_OPEN_ACCESS(URI.create("http://opds-spec.org/acquisition/open-access")),

    /**
     * An item can be sampled.
     */

    ACQUISITION_SAMPLE(URI.create("http://opds-spec.org/acquisition/sample")),

    /**
     * An item can be subscribed to.
     */

    ACQUISITION_SUBSCRIBE(URI.create("http://opds-spec.org/acquisition/subscribe"))
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}
