package org.nypl.simplified.accounts.registry

import android.content.Context
import com.google.common.base.Preconditions
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.http.api.LSHTTPClientType
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.SourceFailed
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.StatusChanged
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.Updated
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus.Idle
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus.Refreshing
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceFactoryType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Collections
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * The default registry of account provider descriptions.
 */

class AccountProviderRegistry private constructor(
  private val context: Context,
  private val sources: List<AccountProviderSourceType>,
  override val defaultProvider: AccountProviderType
) : AccountProviderRegistryType {

  @Volatile
  private var initialized = false

  @Volatile
  private var statusRef: AccountProviderRegistryStatus = Idle

  private val descriptions = Collections.synchronizedMap(LinkedHashMap<URI, AccountProviderDescription>())
  private val descriptionsReadOnly = Collections.unmodifiableMap(this.descriptions)
  private val resolved = ConcurrentHashMap<URI, AccountProviderType>()
  private val resolvedReadOnly = Collections.unmodifiableMap(this.resolved)

  private val logger =
    LoggerFactory.getLogger(AccountProviderRegistry::class.java)

  private val eventsActual: PublishSubject<AccountProviderRegistryEvent> =
    PublishSubject.create()

  override val events: Observable<AccountProviderRegistryEvent> =
    this.eventsActual

  override val status: AccountProviderRegistryStatus
    get() = this.statusRef

  override fun accountProviderDescriptions(): Map<URI, AccountProviderDescription> {
    if (!this.initialized) {
      this.refresh(false)
    }
    return this.descriptionsReadOnly
  }

  override val resolvedProviders: Map<URI, AccountProviderType>
    get() = this.resolvedReadOnly

  override fun refresh(includeTestingLibraries: Boolean) {
    this.logger.debug("refreshing account provider descriptions")

    this.statusRef = Refreshing
    this.eventsActual.onNext(StatusChanged)

    try {
      for (source in this.sources) {
        try {
          when (val result = source.load(this.context, includeTestingLibraries)) {
            is AccountProviderSourceType.SourceResult.SourceSucceeded -> {
              val newDescriptions = result.results
              for (key in newDescriptions.keys) {
                this.updateDescription(newDescriptions[key]!!)
              }
            }
            is AccountProviderSourceType.SourceResult.SourceFailed -> {
              this.eventsActual.onNext(SourceFailed(source.javaClass, result.exception))
            }
          }
        } catch (e: Exception) {
          this.eventsActual.onNext(SourceFailed(source.javaClass, e))
        }
      }
    } finally {
      this.initialized = true
      this.statusRef = Idle
      this.eventsActual.onNext(StatusChanged)
    }
  }

  override fun query(query: AccountSearchQuery) {
    this.logger.debug("refreshing account provider descriptions")

    this.statusRef = Refreshing
    this.eventsActual.onNext(StatusChanged)

    try {
      for (source in this.sources) {
        try {
          when (val result = source.query(this.context, query)) {
            is AccountProviderSourceType.SourceResult.SourceSucceeded -> {
              val newDescriptions = result.results
              for (key in newDescriptions.keys) {
                this.updateDescription(newDescriptions[key]!!)
              }
            }
            is AccountProviderSourceType.SourceResult.SourceFailed -> {
              this.eventsActual.onNext(SourceFailed(source.javaClass, result.exception))
            }
          }
        } catch (e: Exception) {
          this.eventsActual.onNext(SourceFailed(source.javaClass, e))
        }
      }
    } finally {
      this.initialized = true
      this.statusRef = Idle
      this.eventsActual.onNext(StatusChanged)
    }
  }

  override fun clear() {
    this.descriptions.clear()
    this.resolved.clear()
    for (source in this.sources) {
      source.clear(this.context)
    }
  }

  override fun updateProvider(accountProvider: AccountProviderType): AccountProviderType {
    val id = accountProvider.id
    val existing = this.resolved[id]
    if (existing != null) {
      Preconditions.checkState(
        id == existing.id,
        "ID $id must match existing id ${existing.id}"
      )
      if (existing.updated.isAfter(accountProvider.updated)) {
        return existing
      }
    }

    this.logger.debug("received updated version of resolved provider {}", id)
    this.resolved[id] = accountProvider
    this.eventsActual.onNext(Updated(id))

    this.updateDescription(accountProvider.toDescription())
    return accountProvider
  }

  override fun updateDescription(
    description: AccountProviderDescription
  ): AccountProviderDescription {
    val id = description.id
    val existing = this.descriptions[id]
    if (existing != null) {
      Preconditions.checkState(
        id == existing.id,
        "ID $id must match existing id ${existing.id}"
      )
      if (existing.updated.isAfter(description.updated)) {
        return existing
      }
    }

    this.logger.debug("received updated version of description {}", id)
    this.descriptions[id] = description
    this.eventsActual.onNext(Updated(id))
    return description
  }

  override fun resolve(
    onProgress: AccountProviderResolutionListenerType,
    description: AccountProviderDescription
  ): TaskResult<AccountProviderType> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Resolving description...")

    try {
      for (source in this.sources) {
        this.logger.debug("checking source {}", source::class.java.canonicalName)
        if (source.canResolve(description)) {
          val result = source.resolve(onProgress, description)
          taskRecorder.addAll(result.steps)
          return when (result) {
            is TaskResult.Success -> {
              this.updateProvider(result.result)
              this.updateDescription(result.result.toDescription())
              taskRecorder.finishSuccess(result.result)
            }
            is TaskResult.Failure -> taskRecorder.finishFailure()
          }
        }
      }

      taskRecorder.currentStepFailed(
        "No sources can resolve the given description.",
        "noApplicableSource ${description.id} ${description.title}"
      )
      return taskRecorder.finishFailure()
    } catch (e: Exception) {
      this.logger.error("resolution exception: ", e)
      val message = e.message ?: e.javaClass.canonicalName ?: "unknown"
      taskRecorder.currentStepFailedAppending(message, "unexpectedException", e)
      return taskRecorder.finishFailure()
    }
  }

  companion object {

    /**
     * Create a new description registry based on sources discovered by [ServiceLoader]
     */

    fun createFromServiceLoader(
      context: Context,
      http: LSHTTPClientType,
      defaultProvider: AccountProviderType
    ): AccountProviderRegistryType {
      val loader =
        ServiceLoader.load(AccountProviderSourceFactoryType::class.java)
      val buildConfig =
        ServiceLoader.load(BuildConfigurationServiceType::class.java).first()
      val sources =
        loader.toList().map { it.create(context, http, buildConfig) }
      return this.createFrom(context, sources, defaultProvider)
    }

    /**
     * Create a new description registry based on the given list of sources.
     */

    fun createFrom(
      context: Context,
      sources: List<AccountProviderSourceType>,
      defaultProvider: AccountProviderType
    ): AccountProviderRegistry = AccountProviderRegistry(context, sources, defaultProvider)
  }
}
