package org.nypl.simplified.viewer.audiobook.protection

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.librarysimplified.audiobook.json_web_token.JOSEHeader
import org.librarysimplified.audiobook.json_web_token.JSONWebSignature
import org.librarysimplified.audiobook.json_web_token.JSONWebSignatureAlgorithmHMACSha256
import org.librarysimplified.audiobook.json_web_token.JSONWebTokenClaims
import org.nypl.simplified.books.audio.AudioBookCredentials
import org.readium.r2.shared.util.http.HttpRequest
import java.util.UUID

internal class FeedbooksTokenFactory(
  private val configuration: FeedbooksPlayerExtensionConfiguration
) {

  fun createToken(request: HttpRequest): AudioBookCredentials.BearerToken {

    val tokenHeader =
      JOSEHeader(
        mapOf(
          Pair("alg", "HS256"),
          Pair("typ", "JWT")
        )
      )

    val tokenClaims =
      JSONWebTokenClaims(
        mapOf(
          Pair("iss", configuration.issuerURL),
          Pair("sub", request.url),
          Pair("jti", UUID.randomUUID().toString())
        )
      )

    val token =
      JSONWebSignature.create(
        algorithm = JSONWebSignatureAlgorithmHMACSha256.withSecret(
          configuration.bearerTokenSecret
        ),
        header = tokenHeader,
        payload = tokenClaims
      )

    return AudioBookCredentials.BearerToken(token.encode())
  }
}
