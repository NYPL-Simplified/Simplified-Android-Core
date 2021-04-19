package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.net.URI

class AccountListViewModel : ViewModel() {

  private val services = Services.serviceDirectory()

  private val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val subscriptions =
    CompositeDisposable(
      profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent)
    )

  private fun onAccountEvent(event: AccountEvent) {
    accountEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val supportEmailAddress: String =
    buildConfig.supportErrorReportEmailAddress

  val accountEvents: UnicastWorkSubject<AccountEvent> =
    UnicastWorkSubject.create()

  val accounts: List<AccountType>
    get() = this.profilesController.profileCurrent().accounts().values.toList()

  fun deleteAccountByProvider(providerId: URI) {
    this.profilesController.profileAccountDeleteByProvider(providerId)
  }
}
