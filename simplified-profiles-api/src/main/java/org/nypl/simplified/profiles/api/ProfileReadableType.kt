package org.nypl.simplified.profiles.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import java.io.File
import java.net.URI
import java.util.SortedMap

/**
 * The readable interface exposed by profiles.
 *
 * A profile aggregates a display name, a set of accounts, a set of preferences, and a current
 * account. Profiles are assigned monotonically increasing identifiers by the application, but the
 * identifiers themselves carry no meaning. It is an error to depend on the values of identifiers
 * for any kind of program logic. Exactly one account may be current at any given time. It is the
 * responsibility of the application to pick an account provider to be used as the default to derive
 * accounts for newly created profiles.
 */

interface ProfileReadableType : Comparable<ProfileReadableType> {

  /**
   * @return `true` iff this profile is the anonymous profile
   */

  val isAnonymous: Boolean

  /**
   * @return `true` Iff this profile is the current profile
   */

  val isCurrent: Boolean

  /**
   * @return The unique profile identifier
   */

  val id: ProfileID

  /**
   * @return The directory containing the profile's data
   */

  val directory: File

  /**
   * @return The profile's display name
   */

  val displayName: String
    get() = this.description().displayName

  /**
   * @return The profile's current description
   */

  fun description(): ProfileDescription

  /**
   * @return A read-only map of the accounts for this profile
   */

  fun accounts(): SortedMap<AccountID, AccountType>

  /**
   * @return Access to the profile's preferences
   */

  fun preferences(): ProfilePreferences =
    this.description().preferences

  /**
   * @return Access to the profile's attributes
   */

  fun attributes(): ProfileAttributes =
    this.description().attributes

  /**
   * @return A read-only map of the accounts for this profile, organized by provider
   */

  fun accountsByProvider(): SortedMap<URI, AccountType>

  /**
   * @param accountId The account ID
   * @return The account with the given ID
   */

  @Throws(AccountsDatabaseNonexistentException::class)
  fun account(accountId: AccountID): AccountType

  /**
   * @return The most recently used account, or null.
   */

  fun mostRecentAccount(): AccountType {
    return this.preferences().mostRecentAccount.let { accountId ->
      this.account(accountId)
    }
  }
}
