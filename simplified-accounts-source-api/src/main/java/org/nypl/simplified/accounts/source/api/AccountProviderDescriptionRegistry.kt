package org.nypl.simplified.accounts.source.api

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryEvent.*
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ServiceLoader

/**
 * The default registry of account provider descriptions.
 */

class AccountProviderDescriptionRegistry private constructor(
  private val context: Context,
  private val sources: List<AccountProviderSourceType>,
  override val defaultProvider: AccountProviderType) : AccountProviderDescriptionRegistryType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderDescriptionRegistry::class.java)

  @Volatile
  private var initialized = false

  @Volatile
  private var descriptions = mapOf<URI, AccountProviderDescriptionType>()

  private val eventsActual: ObservableType<AccountProviderDescriptionRegistryEvent> =
    Observable.create()

  override val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent> =
    this.eventsActual

  override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
    if (!this.initialized) {
      this.refresh()
    }
    return this.descriptions
  }

  override fun refresh() {
    this.logger.debug("refreshing account provider descriptions")

    val descriptions =
      this.descriptions.toMutableMap()
    val exceptions =
      mutableListOf<java.lang.Exception>()

    for (source in this.sources) {
      try {
        when (val result = source.load(this.context)) {
          is AccountProviderSourceType.SourceResult.SourceSucceeded -> {
            val newDescriptions = result.results
            for (key in newDescriptions.keys) {
              val newDescription = newDescriptions[key]!!
              val oldDescription = descriptions[key]
              if (oldDescription != null) {
                if (newDescription.updated.isBefore(oldDescription.updated)) {
                  this.logger.debug("ignoring older provider {}", key)
                  continue
                }
              }

              descriptions[key] = newDescription
              this.eventsActual.send(Updated(key))
            }
          }
          is AccountProviderSourceType.SourceResult.SourceFailed -> {
            exceptions.add(result.exception)
            this.eventsActual.send(SourceFailed(source.javaClass, result.exception))
          }
        }
      } catch (e: Exception) {
        exceptions.add(e)
        this.eventsActual.send(SourceFailed(source.javaClass, e))
      }
    }

    if (descriptions.isEmpty() && exceptions.isNotEmpty()) {
      throw AccountProviderDescriptionRegistryException(exceptions)
    }

    this.descriptions = descriptions.toMap()
    this.initialized = true
  }

  companion object {

    /**
     * Create a new description registry based on sources discovered by [ServiceLoader]
     */

    fun createFromServiceLoader(
      context: Context,
      defaultProvider: AccountProviderType
    ): AccountProviderDescriptionRegistryType {
      val loader =
        ServiceLoader.load(AccountProviderSourceType::class.java)
      val sources =
        loader.toList()
      return this.createFrom(context, sources, defaultProvider)
    }

    /**
     * Create a new description registry based on the given list of sources.
     */

    fun createFrom(
      context: Context,
      sources: List<AccountProviderSourceType>,
      defaultProvider: AccountProviderType): AccountProviderDescriptionRegistry =
      AccountProviderDescriptionRegistry(context, sources, defaultProvider)

  }
}

