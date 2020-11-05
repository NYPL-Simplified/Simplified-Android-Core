package org.nypl.simplified.feeds.api

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.librarysimplified.http.api.LSHTTPAuthorizationBasic
import org.librarysimplified.http.api.LSHTTPAuthorizationBearerToken
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.http.core.HTTPAuthBasic
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPType
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
) : OPDSFeedTransportType<OptionType<HTTPAuthType>> {

  private val logger =
    LoggerFactory.getLogger(FeedHTTPTransport::class.java)

  @Throws(OPDSFeedTransportException::class)
  override fun getStream(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    method: String
  ): InputStream {
    this.logger.debug("get stream: {} {}", uri, auth)

    val request =
      this.http.newRequest(uri)
        .allowRedirects(LSHTTPRequestBuilderType.AllowRedirects.ALLOW_REDIRECTS)
        .setAuthorization(authorizationOf(auth))
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

  private fun authorizationOf(
    authOpt: OptionType<HTTPAuthType>
  ): LSHTTPAuthorizationType? {
    if (authOpt is Some<HTTPAuthType>) {
      val auth = authOpt.get()
      if (auth is HTTPAuthBasic) {
        return LSHTTPAuthorizationBasic.ofUsernamePassword(
          userName = auth.user(),
          password = auth.password()
        )
      }
      if (auth is HTTPAuthOAuth) {
        return LSHTTPAuthorizationBearerToken.ofToken(auth.token().value())
      }
      throw UnreachableCodeException()
    }
    return null
  }
}
