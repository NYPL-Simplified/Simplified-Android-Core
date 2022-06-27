package org.nypl.simplified.viewer.audiobook.protection

import org.librarysimplified.audiobook.api.PlayerDownloadRequestCredentials.BearerToken
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpTry
import org.slf4j.LoggerFactory

internal class BearerTokenHttpClientCallback(
  private val tokenFactory: (HttpRequest) -> BearerToken,
) : DefaultHttpClient.Callback {

  private val logger =
    LoggerFactory.getLogger(BearerTokenHttpClientCallback::class.java)


  override suspend fun onStartRequest(request: HttpRequest): HttpTry<HttpRequest> {
    this.logger.debug("running bearer token authentication for {}", request.url)

    val token = tokenFactory(request)

    return Try.success(
      request.buildUpon()
        .setHeader("Authorization", token.token)
        .build()
    )
  }
}
