package org.nypl.simplified.feeds.api

import org.librarysimplified.http.api.LSHTTPProblemReport
import org.nypl.simplified.opds.core.OPDSFeedTransportException

/**
 * The type of exceptions caused by HTTP errors.
 */

class FeedHTTPTransportException(
  override val message: String,
  val code: Int,
  val report: LSHTTPProblemReport?
) : OPDSFeedTransportException(message)
