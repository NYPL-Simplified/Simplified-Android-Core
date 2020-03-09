package org.nypl.simplified.tests

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import java.net.URI

class MockAccountProviderRegistry : AccountProviderRegistryType {

  val eventSource = PublishSubject.create<AccountProviderRegistryEvent>()

  override val events: Observable<AccountProviderRegistryEvent>
    get() = eventSource

  override val defaultProvider: AccountProviderType
    get() = MockAccountProviders.fakeAuthProvider("urn:0")

  override val resolvedProviders: Map<URI, AccountProviderType>
    get() = mapOf()

  override val status: AccountProviderRegistryStatus
    get() = AccountProviderRegistryStatus.Idle

  override fun refresh() {
  }

  override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
    return IntRange(0, 30)
      .toList()
      .map { index -> MockAccountProviders.fakeAuthProvider("urn:$index") }
      .map { prov -> prov.toDescription() }
      .map { prov -> Pair(prov.metadata.id, prov) }
      .toMap()
  }

  override fun updateProvider(accountProvider: AccountProviderType): AccountProviderType {
    return accountProvider
  }

  override fun updateDescription(description: AccountProviderDescriptionType): AccountProviderDescriptionType {
    return description
  }
}
