package org.nypl.simplified.announcements

import org.joda.time.LocalDateTime
import java.util.UUID

/**
 * A single announcement.
 */

data class Announcement(
  val id: UUID,
  val content: String,
  val expires: LocalDateTime?
)
