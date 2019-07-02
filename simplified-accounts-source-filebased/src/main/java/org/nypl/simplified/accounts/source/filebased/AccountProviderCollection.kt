package org.nypl.simplified.accounts.source.filebased

import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.accounts.api.AccountProviderType
import java.lang.IllegalArgumentException
import java.net.URI
import java.util.SortedMap

internal class AccountProviderCollection(
  private val providerDefault: AccountProviderType,
  private val providers: SortedMap<URI, AccountProviderType>) : AccountProviderCollectionType {

  override fun providerDefault(): AccountProviderType =
    this.providerDefault

  override fun providers(): SortedMap<URI, AccountProviderType> =
    this.providers

  override fun provider(providerID: URI): AccountProviderType =
    this.providers[providerID] ?: throw IllegalArgumentException("No such provider: $providerID")
}