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
    return mergeAttributes(map, problemReportAsAttributes(problemReport))
  }

  /**
   * Merge the given problem report as a set of attributes into the existing attributes.
   */

  fun problemReportAsAttributes(
    problemReport: HTTPProblemReport?
  ): Map<String, String> {
    return when (problemReport) {
      null -> mapOf()
      else -> {
        val attributes = mutableMapOf<String, String>()
        attributes["HTTP problem detail"] = problemReport.problemDetail ?: ""
        attributes["HTTP problem status"] = problemReport.problemStatus.toString()
        attributes["HTTP problem title"] = problemReport.problemTitle ?: ""
        attributes["HTTP problem type"] = problemReport.problemType.toString()
        attributes.toMap()
      }
    }
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
   * Merge all of the attributes of `map0` into `map`, making names unique as necessary.
   */

  fun mergeAttributes(
    map0: Map<String, String>,
    map1: Map<String, String>
  ): Map<String, String> {
    val attributes = mutableMapOf<String, String>()
    attributes.putAll(map0)
    for ((key, value) in map1) {
      putRetry(attributes, key, value)
    }
    return attributes.toMap()
  }

  /**
   * Try to put the given key/value into the map, making a reasonable effort to make keys unique
   * in the presence of duplicates.
   */

  fun putRetry(
    attributes: MutableMap<String, String>,
    key: String,
    value: String
  ) {
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
