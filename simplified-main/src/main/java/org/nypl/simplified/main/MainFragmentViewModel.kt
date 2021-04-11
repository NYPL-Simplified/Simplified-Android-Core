package org.nypl.simplified.main

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * The view model for the main fragment.
 */

class MainFragmentViewModel : ViewModel() {

  val accountEvents: UnicastWorkSubject<AccountEvent> =
    UnicastWorkSubject.create()
  val profileEvents: UnicastWorkSubject<ProfileEvent> =
    UnicastWorkSubject.create()

  private val services =
    Services.serviceDirectory()
  val profilesController =
    services.requireService(ProfilesControllerType::class.java)
  val accountProviders: AccountProviderRegistryType =
    services.requireService(AccountProviderRegistryType::class.java)
  val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val subscriptions: CompositeDisposable =
    CompositeDisposable()

  init {
    profilesController.accountEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onAccountEvent)
      .let { subscriptions.add(it) }
  }

  private fun onAccountEvent(event: AccountEvent) {
    accountEvents.onNext(event)
  }

  init {
    this.profilesController.profileEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onProfileEvent)
      .let { subscriptions.add(it) }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    profileEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }
}
