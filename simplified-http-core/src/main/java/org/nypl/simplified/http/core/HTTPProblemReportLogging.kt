package org.nypl.simplified.http.core

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.slf4j.Logger
import java.net.URI

/**
 * Functions to log HTTP errors.
 */

object HTTPProblemReportLogging {

  /**
   * Log the given HTTP error.
   *
   * @param logger The target logger
   * @param uri The original URI
   * @param message The error message
   * @param statusCode The HTTP status code
   * @param reportOption The problem report, if one exists
   */

  fun logError(
    logger: Logger,
    uri: URI,
    message: String,
    statusCode: Int,
    reportOption: OptionType<HTTPProblemReport>
  ): String {
    val text =
      StringBuilder(128)
        .append("Error retrieving URI\n")
        .append("  URI:     ")
        .append(uri)
        .append("\n")
        .append("  Message: ")
        .append(message)
        .append("\n")
        .append("  Status:  ")
        .append(statusCode)
        .append("\n")

    if (reportOption.isSome) {
      val report = (reportOption as Some<HTTPProblemReport>).get()
      logger.error(
        "{}",
        text.append("  Report:\n")
          .append("    Status: ")
          .append(report.problemStatus)
          .append("\n")
          .append("    Type:   ")
          .append(report.problemType)
          .append("\n")
          .append("    Title:  ")
          .append(report.problemTitle)
          .append("\n")
          .append("    Detail: ")
          .append(report.problemDetail)
          .append("\n")
          .toString()
      )
    } else {
      logger.error(
        "{}",
        text.append("  Report: No problem report available\n")
          .append("\n")
          .toString()
      )
    }

    return text.toString()
  }
}
