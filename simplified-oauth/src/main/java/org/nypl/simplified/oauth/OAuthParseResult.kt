package org.nypl.simplified.oauth

import java.util.UUID

sealed class OAuthParseResult {

  data class Failed(
    val message: String
  ) : OAuthParseResult()

  data class Success(
    val accountId: UUID,
    val token: String
  ) : OAuthParseResult()
}
