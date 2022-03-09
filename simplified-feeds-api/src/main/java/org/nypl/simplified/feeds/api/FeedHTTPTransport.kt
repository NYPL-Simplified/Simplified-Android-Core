package org.nypl.simplified.feeds.api

import one.irradia.mime.api.MIMECompatibility
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.accounts.api.setAuthentication
import org.nypl.simplified.opds.core.OPDSFeedTransportException
import org.nypl.simplified.opds.core.OPDSFeedTransportIOException
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Locale

/**
 * An implementation of the [OPDSFeedTransportType] interface that uses an
 * [HTTPType] instance for communication, supporting optional
 * authentication.
 */

class FeedHTTPTransport(
  private val http: LSHTTPClientType
) : OPDSFeedTransportType<AccountReadableType> {

  private val logger =
    LoggerFactory.getLogger(FeedHTTPTransport::class.java)

  @Throws(OPDSFeedTransportException::class)
  override fun getStream(
    account: AccountReadableType,
    uri: URI,
    method: String,
    authenticate: Boolean
  ): InputStream {
    this.logger.debug("get stream: {} {}", uri, account)

    val request =
      this.http.newRequest(uri)
        .setMethod(this.methodOfName(method))
        .apply { if (authenticate) { setAuthentication(account) } }
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        status.bodyStream ?: ByteArrayInputStream(ByteArray(0))

      is LSHTTPResponseStatus.Responded.Error ->
        throw FeedHTTPTransportException(
          message = status.properties.message,
          code = status.properties.status,
          report = status.properties.problemReport
        )

      is LSHTTPResponseStatus.Failed ->
        throw OPDSFeedTransportIOException(
          message = "Connection failed",
          cause = IOException(status.exception)
        )
    }
  }

  private fun methodOfName(method: String): LSHTTPRequestBuilderType.Method {
    return when (method.toUpperCase(Locale.ROOT)) {
      "GET" -> LSHTTPRequestBuilderType.Method.Get
      "HEAD" -> LSHTTPRequestBuilderType.Method.Head
      "POST" -> LSHTTPRequestBuilderType.Method.Post(ByteArray(0), MIMECompatibility.applicationOctetStream)
      "PUT" -> LSHTTPRequestBuilderType.Method.Put(ByteArray(0), MIMECompatibility.applicationOctetStream)
      "DELETE" -> LSHTTPRequestBuilderType.Method.Delete
      else -> throw IllegalArgumentException("Unsupported request method: $method")
    }
  }
}
