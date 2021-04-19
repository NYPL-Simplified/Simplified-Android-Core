package org.nypl.simplified.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.MoreExecutors
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.net.URI

class SettingsCustomOPDSViewModel : ViewModel() {

  private val services: ServiceDirectoryType =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val subscriptions = CompositeDisposable(
    profilesController
      .accountEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onAccountEvent)
  )

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  private fun onAccountEvent(event: AccountEvent) {
    accountEvents.onNext(event)
  }

  val accountEvents: UnicastWorkSubject<AccountEvent> =
    UnicastWorkSubject.create()

  val taskRunning: MutableLiveData<Boolean> =
    MutableLiveData(false)

  fun createCustomOPDSFeed(uri: String) {
    taskRunning.value = true

    val future = this.profilesController
      .profileAccountCreateCustomOPDS(URI(uri))

    future.addListener(
      { this.taskRunning.postValue(false) },
      MoreExecutors.directExecutor()
    )
  }
}
