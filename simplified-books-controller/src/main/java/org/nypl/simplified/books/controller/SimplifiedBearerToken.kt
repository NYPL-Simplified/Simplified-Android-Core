package org.nypl.simplified.books.controller

import org.joda.time.LocalDateTime
import java.net.URI

/**
 * The type of bearer tokens.
 */

data class SimplifiedBearerToken(
  val accessToken: String,
  val expiration: LocalDateTime,
  val location: URI
)
