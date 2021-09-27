package org.nypl.simplified.tests.mocking

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.LinkedList
import java.util.Queue

class MockAccountProviderRegistry(
  override val defaultProvider: AccountProviderType = MockAccountProviders.fakeAuthProvider("urn:0"),
  override val resolvedProviders: MutableMap<URI, AccountProviderType>,
  val descriptions: MutableMap<URI, AccountProviderDescription>
) : AccountProviderRegistryType {

  private val logger =
    LoggerFactory.getLogger(MockAccountProviderRegistry::class.java)

  val resolveNext: Queue<AccountProviderType> =
    LinkedList<AccountProviderType>()

  fun returnForNextResolution(accountProvider: AccountProviderType) {
    this.resolveNext.add(accountProvider)
  }

  companion object {
    fun withProviders(accountProviders: List<AccountProviderType>): MockAccountProviderRegistry {
      return MockAccountProviderRegistry(
        accountProviders[0],
        accountProviders.toList().associateBy(AccountProviderType::id).toMutableMap(),
        mutableMapOf()
      )
    }

    fun withProviders(vararg accountProviders: AccountProviderType): MockAccountProviderRegistry {
      return MockAccountProviderRegistry(
        accountProviders[0],
        accountProviders.toList().associateBy(AccountProviderType::id).toMutableMap(),
        mutableMapOf()
      )
    }

    fun singleton(accountProvider: AccountProviderType): MockAccountProviderRegistry {
      return withProviders(accountProvider)
    }
  }

  val eventSource = PublishSubject.create<AccountProviderRegistryEvent>()

  override val events: Observable<AccountProviderRegistryEvent>
    get() = eventSource

  override val status: AccountProviderRegistryStatus
    get() = AccountProviderRegistryStatus.Idle

  override fun refresh(includeTestingLibraries: Boolean) {
  }

  override fun query(query: AccountSearchQuery) {
  }

  override fun clear() {
  }

  override fun accountProviderDescriptions(): Map<URI, AccountProviderDescription> {
    return this.resolvedProviders.mapValues { p -> p.value.toDescription() }
  }

  override fun updateProvider(accountProvider: AccountProviderType): AccountProviderType {
    this.logger.debug("updateProvider: {}", accountProvider)
    this.resolvedProviders[accountProvider.id] = accountProvider
    return accountProvider
  }

  override fun updateDescription(description: AccountProviderDescription): AccountProviderDescription {
    this.logger.debug("updateDescription: {}", description)
    this.descriptions.put(description.id, description)
    return description
  }

  override fun resolve(
    onProgress: AccountProviderResolutionListenerType,
    description: AccountProviderDescription
  ): TaskResult<AccountProviderType> {
    this.logger.debug("resolve: {}", description)

    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Resolving account provider...")

    if (this.resolveNext.peek() != null) {
      this.logger.debug("took provider from queue")
      val queued = this.resolveNext.poll()
      val copy = AccountProvider.copy(queued).copy(id = description.id)
      this.resolvedProviders[copy.id] = copy
      return taskRecorder.finishSuccess(copy)
    }

    this.logger.debug("taking provider from map")
    val provider = this.resolvedProviders[description.id]
    return if (provider == null) {
      this.logger.debug("no provider in map")
      taskRecorder.currentStepFailed("Failed", "unexpectedException")
      taskRecorder.finishFailure()
    } else {
      this.logger.debug("took provider from map")
      taskRecorder.finishSuccess(provider)
    }
  }
}
