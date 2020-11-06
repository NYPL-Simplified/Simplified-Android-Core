package org.nypl.simplified.feeds.api

import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.opds.core.OPDSFeedTransportException
import org.nypl.simplified.opds.core.OPDSFeedTransportIOException
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * An implementation of the [OPDSFeedTransportType] interface that uses an
 * [HTTPType] instance for communication, supporting optional
 * authentication.
 */

class FeedHTTPTransport(
  private val http: LSHTTPClientType
) : OPDSFeedTransportType<LSHTTPAuthorizationType?> {

  private val logger =
    LoggerFactory.getLogger(FeedHTTPTransport::class.java)

  @Throws(OPDSFeedTransportException::class)
  override fun getStream(
    auth: LSHTTPAuthorizationType?,
    uri: URI,
    method: String
  ): InputStream {
    this.logger.debug("get stream: {} {}", uri, auth)

    val request =
      this.http.newRequest(uri)
        .setAuthorization(auth)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        status.bodyStream ?: ByteArrayInputStream(ByteArray(0))

      is LSHTTPResponseStatus.Responded.Error ->
        throw FeedHTTPTransportException(
          message = status.message,
          code = status.status,
          report = status.problemReport
        )

      is LSHTTPResponseStatus.Failed ->
        throw OPDSFeedTransportIOException(
          message = "Connection failed",
          cause = IOException(status.exception)
        )
    }
  }
}
