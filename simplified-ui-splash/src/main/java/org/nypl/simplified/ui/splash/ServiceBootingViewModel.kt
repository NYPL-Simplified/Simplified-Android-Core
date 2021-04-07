package org.nypl.simplified.ui.splash

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.boot.api.BootEvent
import java.util.ServiceLoader

class ServiceBootingViewModel : ViewModel() {

  private val splashDependencies = ServiceLoader
    .load(SplashListenerType::class.java)
    .firstOrNull()
    ?: throw IllegalStateException(
      "No available services of type ${SplashListenerType::class.java.canonicalName}"
    )

  val bootFuture =
    splashDependencies.onSplashWantBootFuture()

  val bootEvents: UnicastWorkSubject<BootEvent> =
    UnicastWorkSubject.create()

  private val subscriptions =
    CompositeDisposable()

  init {
    splashDependencies
      .onSplashWantBootEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { bootEvents.onNext(it) }
      .let { subscriptions.add(it) }
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }
}
