package org.nypl.simplified.ui.accounts

import android.Manifest
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.api.AccountGeoLocation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class AccountListViewModel(private val locationManager: LocationManager) : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val accountRegistry =
    services.requireService(AccountProviderRegistryType::class.java)

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val logger =
    LoggerFactory.getLogger(AccountListViewModel::class.java)

  private val queries = BehaviorSubject.create<String>()

  private val isLoadingSubject = BehaviorSubject.createDefault(true)
  val isLoading: Observable<Boolean>
    get() = Observable.combineLatest(
      isLoadingSubject, accountCreationEvents.map(::checkIsLoading).startWith(false)
    ) { t1, t2 -> t1 || t2 }.observeOn(AndroidSchedulers.mainThread())
  private val accountsSubject =
    BehaviorSubject.createDefault<List<AccountProviderDescription>>(emptyList())
  val accounts: Observable<List<AccountProviderDescription>>
    get() = accountsSubject.hide().observeOn(AndroidSchedulers.mainThread())
  private val registeredAccountsSubject =
    BehaviorSubject.createDefault<List<AccountType>>(emptyList())
  val registeredAccounts: Observable<List<AccountType>>
    get() = registeredAccountsSubject.hide().observeOn(AndroidSchedulers.mainThread())
  private val registeredDeletionFailureSubject = PublishSubject.create<Unit>()
  val registeredDeletionFailures: Observable<Unit>
    get() = registeredDeletionFailureSubject.hide().observeOn(AndroidSchedulers.mainThread())
  private val showMyAccountSubject = BehaviorSubject.create<Boolean>()
  val showMyAccount: Observable<Boolean>
    get() = showMyAccountSubject.hide().observeOn(AndroidSchedulers.mainThread())

  private val subscriptions: CompositeDisposable =
    CompositeDisposable(
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent),
      queries.debounce(250L, TimeUnit.MILLISECONDS).distinctUntilChanged()
        .map(::createQuery)
        .switchMapCompletable(this::executeQuery)
        .subscribeOn(Schedulers.io())
        .onErrorComplete()
        .subscribe(),
      profilesController.accountEvents().subscribe(this::onRegisteredAccountEvent),
      Single.fromCallable { this.profilesController.profileCurrent().accounts().values.toList() }
        .map { it.sortedWith(AccountComparator()) }
        .subscribeOn(Schedulers.io())
        .subscribe(registeredAccountsSubject::onNext),
      this.accountRegistry.events
        .filter(this::shouldFetchAccountList)
        .map { determineAvailableAccountProviderDescriptions() }
        .subscribeOn(Schedulers.io())
        .subscribe(this::displayAccounts),
      Single.fromCallable {
        this.profilesController.profileCurrent().preferences().hasSeenLibrarySelectionScreen
      }.subscribeOn(Schedulers.io()).subscribe(showMyAccountSubject::onNext)
    )

  private fun onAccountEvent(event: AccountEvent) {
    this.accountCreationEventsSubject.onNext(event)
  }

  private fun shouldFetchAccountList(event: AccountProviderRegistryEvent): Boolean {
    return event is AccountProviderRegistryEvent.StatusChanged && this.accountRegistry.status is AccountProviderRegistryStatus.Idle
  }

  override fun onCleared() {
    super.onCleared()
    this.subscriptions.clear()
  }

  val supportEmailAddress = this.buildConfig.supportErrorReportEmailAddress

  private val accountCreationEventsSubject: Subject<AccountEvent> = BehaviorSubject.create()
  val accountCreationEvents: Observable<AccountEvent>
    get() = accountCreationEventsSubject.hide()

  private var activeLocation: Location? = null
    set(value) {
      field = value
      if (value != null)
        clearQuery()
    }

  fun refreshAccountRegistry() {
    Completable.fromAction {
      this.accountRegistry.refresh(
        includeTestingLibraries = this.profilesController
          .profileCurrent()
          .preferences()
          .showTestingLibraries
      )
    }.doOnError { this.logger.error("failed to refresh registry: ", it) }
      .onErrorComplete()
      .subscribeOn(Schedulers.io())
      .subscribe().let(subscriptions::add)
  }

  fun createAccount(id: URI) {
    this.profilesController.profileAccountCreate(id)
  }

  fun deleteAccountByProvider(providerId: URI) {
    this.profilesController.profileAccountDeleteByProvider(providerId)
  }

  fun clearQuery() = query("")
  fun query(query: String) = queries.onNext(query)

  @RequiresPermission(value = Manifest.permission.ACCESS_COARSE_LOCATION)
  fun getLocation(hasPermission: Boolean) {
    if (hasPermission) {
      tryAndGetLocation()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe()
        .let(subscriptions::add)
    } else {
      isLoadingSubject.onNext(false)
    }
  }

  /**
   * Return a list of the available account providers. An account provider is available
   * if no account already exists for it in the current profile.
   */

  private fun determineAvailableAccountProviderDescriptions(): List<AccountProviderDescription> {
    val usedAccountProviders =
      this.profilesController
        .profileCurrentlyUsedAccountProviders()
        .map { p -> p.toDescription() }

    this.logger.debug("profile is using {} providers", usedAccountProviders.size)

    val availableAccountProviders = this.accountRegistry.accountProviderDescriptions()
      .values
      .toMutableList()
    availableAccountProviders.removeAll(usedAccountProviders)

    this.logger.debug("returning {} available providers", availableAccountProviders.size)
    return availableAccountProviders
  }

  @RequiresPermission(value = Manifest.permission.ACCESS_COARSE_LOCATION)
  private fun tryAndGetLocation(): Single<Boolean> = Single.fromCallable {
    val isNetworkLocationEnabled =
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    if (isNetworkLocationEnabled) {
      activeLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }
    activeLocation == null
  }

  private fun createQuery(query: String) = AccountSearchQuery(
    location = activeLocation?.toAccountGeoLocation(),
    searchQuery = query,
    includeTestingLibraries = this.profilesController
      .profileCurrent()
      .preferences()
      .showTestingLibraries
  )

  private fun executeQuery(query: AccountSearchQuery) = Completable.fromAction {
    isLoadingSubject.onNext(true)
    accountRegistry.query(query)
  }

  private fun Location.toAccountGeoLocation() = AccountGeoLocation.Coordinates(
    longitude, latitude
  )

  private fun onRegisteredAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventDeletion.AccountEventDeletionFailed -> {
        registeredDeletionFailureSubject.onNext(Unit)
      }
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded,
      is AccountEventUpdated -> {
        val accounts = this.profilesController.profileCurrent().accounts().values.toList()
          .sortedWith(AccountComparator())
        registeredAccountsSubject.onNext(accounts)
      }
    }
  }

  private fun displayAccounts(accounts: List<AccountProviderDescription>) {
    accountsSubject.onNext(accounts)
    isLoadingSubject.onNext(false)
  }

  private fun checkIsLoading(accountEvent: AccountEvent): Boolean {
    return accountEvent is AccountEventDeletion.AccountEventDeletionInProgress ||
      accountEvent is AccountEventCreation.AccountEventCreationInProgress
  }
}
