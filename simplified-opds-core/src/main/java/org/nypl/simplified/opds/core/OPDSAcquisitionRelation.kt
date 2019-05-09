package org.nypl.simplified.opds.core

import java.net.URI

/**
 * An acquisition relation.
 */

enum class OPDSAcquisitionRelation(val uri: URI) {

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