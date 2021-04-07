package org.nypl.simplified.ui.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.MoreExecutors
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport
import java.util.ServiceLoader
import java.util.concurrent.Future

class MigrationsViewModel : ViewModel() {

  private val splashDependencies = ServiceLoader
    .load(SplashListenerType::class.java)
    .firstOrNull()
    ?: throw IllegalStateException(
      "No available services of type ${SplashListenerType::class.java.canonicalName}"
    )

  private val migrations =
    splashDependencies.onSplashWantMigrations()

  val migrationEvents: UnicastWorkSubject<MigrationEvent> =
    UnicastWorkSubject.create()

  private val subscriptions =
    CompositeDisposable()

  init {
    migrations.events
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { migrationEvents.onNext(it) }
      .let { subscriptions.add(it) }
  }

  val migrationsCompleted = MutableLiveData(false)

  fun startMigrations(): Future<MigrationReport?> {
    val future = migrations.start()

    future.addListener(
      { migrationsCompleted.postValue(true) },
      MoreExecutors.directExecutor()
    )

    return future
  }

  fun anyMigrationNeedToRun(): Boolean {
   return migrations.anyNeedToRun()
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }
}
