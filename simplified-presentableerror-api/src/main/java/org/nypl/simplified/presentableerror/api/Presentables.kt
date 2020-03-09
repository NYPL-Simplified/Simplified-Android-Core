package org.nypl.simplified.presentableerror.api

import org.nypl.simplified.http.core.HTTPProblemReport

/**
 * Functions over presentable values.
 */

object Presentables {

  /**
   * Merge the given problem report as a set of attributes into the existing attributes.
   */

  fun mergeProblemReport(
    map: Map<String, String>,
    problemReport: HTTPProblemReport
  ): Map<String, String> {
    val attributes = map.toMutableMap()
    putRetry(attributes, "HTTP problem detail", problemReport.problemDetail)
    putRetry(attributes, "HTTP problem status", problemReport.problemStatus.toString())
    putRetry(attributes, "HTTP problem title", problemReport.problemTitle)
    putRetry(attributes, "HTTP problem type", problemReport.problemType.toString())
    return attributes.toMap()
  }

  /**
   * Merge the given problem report as a set of attributes into the existing attributes.
   */

  fun mergeProblemReportOptional(
    map: Map<String, String>,
    problemReport: HTTPProblemReport?
  ): Map<String, String> {
    return problemReport?.let { report -> mergeProblemReport(map, report) } ?: map
  }

  /**
   * Collect all of the attributes of all of the given presentable values, making keys unique
   * as necessary.
   */

  fun collectAttributes(presentables: List<PresentableType>): Map<String, String> {
    val attributes = mutableMapOf<String, String>()

    for (presentable in presentables) {
      for ((key, value) in presentable.attributes) {
        putRetry(attributes, key, value)
      }
    }

    return attributes.toMap()
  }

  /**
   * Try to put the given key/value into the map, making a reasonable effort to make keys unique
   * in the presence of duplicates.
   */

  private fun putRetry(attributes: MutableMap<String, String>, key: String, value: String) {
    if (!attributes.containsKey(key)) {
      attributes[key] = value
      return
    }

    for (i in 0..100) {
      val incKey = "$key ($i)"
      if (!attributes.containsKey(incKey)) {
        attributes[incKey] = value
        return
      }
    }
  }
}
