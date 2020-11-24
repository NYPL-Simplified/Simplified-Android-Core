package org.nypl.simplified.books.audio

import com.google.common.base.Preconditions

/**
 * Credentials used when fulfilling audio books.
 */

sealed class AudioBookCredentials {

  /**
   * Credentials represented by a username and password.
   */

  data class UsernamePassword(
    val userName: String,
    val password: String
  ) : AudioBookCredentials() {
    init {
      Preconditions.checkArgument(
        password.isNotBlank(),
        "Password cannot be empty/blank (Use the UsernameOnly) class."
      )
    }
  }

  /**
   * Credentials represented by a username. Some audio book backends require an explicit
   * indication that there is no password. This class must be used to provide that indication.
   */

  data class UsernameOnly(
    val userName: String
  ) : AudioBookCredentials()

  /**
   * Credentials represented by a bearer token.
   */

  data class BearerToken(
    val accessToken: String
  ) : AudioBookCredentials()
}
