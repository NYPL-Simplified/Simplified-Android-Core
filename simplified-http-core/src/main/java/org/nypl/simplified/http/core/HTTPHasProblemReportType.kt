package org.nypl.simplified.http.core

/**
 * An interface that can be implemented to indicate that an HTTP problem report might be included.
 */

interface HTTPHasProblemReportType {

  /**
   * Retrieve the problem report.
   */

  val problemReport: HTTPProblemReport?

}
