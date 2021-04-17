package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.LoggerFactory
import java.net.URI

class AccountListRegistryViewModel : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val accountRegistry =
    services.requireService(AccountProviderRegistryType::class.java)

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val backgroundExecutor =
    NamedThreadPools.namedThreadPool(1, "simplified-registry-io", 19)

  private val logger =
    LoggerFactory.getLogger(AccountListRegistryViewModel::class.java)

  private val subscriptions: CompositeDisposable =
    CompositeDisposable(
      this.accountRegistry.events
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountRegistryEvent),
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent)
    )

  private fun onAccountRegistryEvent(event: AccountProviderRegistryEvent) {
    this.accountRegistryEvents.onNext(event)
  }

  private fun onAccountEvent(event: AccountEvent) {
    this.accountCreationEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    this.subscriptions.clear()
    this.backgroundExecutor.shutdown()
  }

  val supportEmailAddress =
    this.buildConfig.supportErrorReportEmailAddress

  val accountRegistryEvents: UnicastWorkSubject<AccountProviderRegistryEvent> =
    UnicastWorkSubject.create()

  val accountCreationEvents: UnicastWorkSubject<AccountEvent> =
    UnicastWorkSubject.create()

  val accountRegistryStatus: AccountProviderRegistryStatus
    get() = this.accountRegistry.status

  fun refreshAccountRegistry() {
    this.backgroundExecutor.execute {
      try {
        this.accountRegistry.refresh(
          includeTestingLibraries = this.profilesController
            .profileCurrent()
            .preferences()
            .showTestingLibraries
        )
      } catch (e: Exception) {
        this.logger.error("failed to refresh registry: ", e)
      }
    }
  }

  fun createAccount(id: URI) {
    this.profilesController.profileAccountCreate(id)
  }

  /**
   * Return a list of the available account providers. An account provider is available
   * if no account already exists for it in the current profile.
   */

  fun determineAvailableAccountProviderDescriptions(): List<AccountProviderDescription> {
    val usedAccountProviders =
      this.profilesController
        .profileCurrentlyUsedAccountProviders()
        .map { p -> p.toDescription() }

    this.logger.debug("profile is using {} providers", usedAccountProviders.size)

    val availableAccountProviders =
      this.accountRegistry.accountProviderDescriptions()
        .values
        .toMutableList()
    availableAccountProviders.removeAll(usedAccountProviders)

    this.logger.debug("returning {} available providers", availableAccountProviders.size)
    return availableAccountProviders
  }
}
