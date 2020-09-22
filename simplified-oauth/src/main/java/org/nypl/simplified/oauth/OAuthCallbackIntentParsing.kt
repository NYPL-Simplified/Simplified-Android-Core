package org.nypl.simplified.oauth

import android.content.Intent
import android.net.Uri
import org.slf4j.LoggerFactory
import java.util.UUID

object OAuthCallbackIntentParsing {

  private val logger =
    LoggerFactory.getLogger(OAuthCallbackIntentParsing::class.java)

  fun createUri(
    requiredScheme: String,
    accountId: UUID
  ): String {
    return buildString {
      this.append(requiredScheme)
      this.append("://")
      this.append(accountId)
      this.append("@authenticated")
    }
  }

  fun processIntent(
    intent: Intent,
    requiredScheme: String,
    parseUri: (String) -> Uri
  ): OAuthParseResult {
    return processUri(
      data = intent.data ?: return OAuthParseResult.Failed("No data provided with intent"),
      requiredScheme = requiredScheme,
      parseUri = parseUri
    )
  }

  fun processUri(
    data: Uri,
    requiredScheme: String,
    parseUri: (String) -> Uri
  ): OAuthParseResult {
    val scheme =
      data.scheme ?: return OAuthParseResult.Failed("Unrecognized URI used in intent: $data")
    val user =
      data.userInfo ?: return OAuthParseResult.Failed("No user info in intent: $data")

    if (scheme != requiredScheme) {
      return OAuthParseResult.Failed("Unrecognized URI scheme used in intent: $data")
    }

    return try {
      val accountId =
        UUID.fromString(user)
      val fakeURI =
        parseUri.invoke(
          buildString {
            this.append(requiredScheme)
            this.append("://")
            this.append(accountId)
            this.append("@authenticated?")
            this.append(data.encodedFragment)
          }
        )
      val value =
        fakeURI.getQueryParameter("access_token")

      if (value == null) {
        OAuthParseResult.Failed("Response did not contain an access_token parameter.")
      } else {
        OAuthParseResult.Success(
          accountId = accountId,
          token = value
        )
      }
    } catch (e: Exception) {
      this.logger.error("failure parsing intent: ", e)
      OAuthParseResult.Failed("Failed to parse intent: " + e.message)
    }
  }
}
