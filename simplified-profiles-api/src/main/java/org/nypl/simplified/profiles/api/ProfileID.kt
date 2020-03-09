package org.nypl.simplified.profiles.api

import java.io.Serializable
import java.util.UUID

/**
 * A unique identifier for a profile.
 */

data class ProfileID(
  val uuid: UUID
) : Serializable, Comparable<ProfileID> {

  override fun compareTo(other: ProfileID): Int =
    this.uuid.compareTo(other.uuid)

  override fun toString(): String =
    this.uuid.toString()

  companion object {

    /**
     * Generate a random profile ID.
     */

    fun generate(): ProfileID =
      ProfileID(UUID.randomUUID())
  }
}
