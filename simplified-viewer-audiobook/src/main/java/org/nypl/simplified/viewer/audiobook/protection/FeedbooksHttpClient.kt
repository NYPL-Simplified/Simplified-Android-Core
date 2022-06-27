package org.nypl.simplified.viewer.audiobook.protection

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient

internal class FeedbooksHttpClientFactory(
  private val configuration: FeedbooksPlayerExtensionConfiguration,
) {

  fun createHttpClient(): HttpClient {

    val tokenFactory = FeedbooksTokenFactory(configuration)::createToken

    return DefaultHttpClient(
      callback = BearerTokenHttpClientCallback(tokenFactory)
    )
  }
}
