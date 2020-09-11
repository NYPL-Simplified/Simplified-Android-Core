package org.nypl.simplified.books.borrowing.internal

import org.librarysimplified.http.api.LSHTTPProblemReport

/**
 * Convenience functions over HTTP.
 */

object BorrowHTTP {

  /**
   * Encode the given problem report as a set of presentable attributes.
   */

  fun problemReportAsAttributes(
    problemReport: LSHTTPProblemReport?
  ): Map<String, String> {
    return when (problemReport) {
      null -> mapOf()
      else -> {
        val attributes = mutableMapOf<String, String>()
        attributes["HTTP problem detail"] = problemReport.detail ?: ""
        attributes["HTTP problem status"] = problemReport.status.toString()
        attributes["HTTP problem title"] = problemReport.title ?: ""
        attributes["HTTP problem type"] = problemReport.type.toString()
        attributes.toMap()
      }
    }
  }
}
