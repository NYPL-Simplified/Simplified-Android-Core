package org.nypl.simplified.accounts.source.api

import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.observable.ObservableReadableType
import java.net.URI

/**
 * The interface exposing a set of account provider descriptions.
 */

interface AccountProviderDescriptionRegistryType {

  /**
   * A source of registry events.
   */

  val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent>

  /**
   * The default, guaranteed-to-exist account provider.
   */

  val defaultProvider: AccountProviderType

  /**
   * Refresh the available account providers from all sources.
   *
   * @throws AccountProviderDescriptionRegistryException If one or more sources failed _and_ the resulting set of account providers is empty
   *
   */

  @Throws(AccountProviderDescriptionRegistryException::class)
  fun refresh()

  /**
   * Return an immutable snapshot of the current account provider descriptions.
   *
   * Implementations are required to implicitly call [refresh] if the method has not previously
   * been called.
   */

  @Throws(AccountProviderDescriptionRegistryException::class)
  fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType>

  /**
   * Find the account provider with the given `id`.
   *
   * Implementations are required to implicitly call [refresh] if the method has not previously
   * been called.
   */

  @Throws(AccountProviderDescriptionRegistryException::class)
  fun findAccountProviderDescription(id: URI): AccountProviderDescriptionType? =
    this.accountProviderDescriptions()[id]
}
