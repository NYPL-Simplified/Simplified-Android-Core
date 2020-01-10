package org.nypl.simplified.accounts.api

import java.io.Serializable
import java.util.UUID

/**
 * A unique identifier for an account.
 */

data class AccountID(
  val uuid: UUID
) : Serializable, Comparable<AccountID> {

  override fun compareTo(other: AccountID): Int =
    this.uuid.compareTo(other.uuid)

  override fun toString(): String =
    this.uuid.toString()

  companion object {

    /**
     * Generate a random account ID.
     */

    fun generate(): AccountID =
      AccountID(UUID.randomUUID())
  }
}
