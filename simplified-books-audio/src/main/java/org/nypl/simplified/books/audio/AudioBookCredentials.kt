package org.nypl.simplified.books.audio

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
  ) : AudioBookCredentials()
}
