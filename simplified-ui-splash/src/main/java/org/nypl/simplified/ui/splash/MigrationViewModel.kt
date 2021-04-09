package org.nypl.simplified.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.MoreExecutors
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport
import java.util.concurrent.Future

class MigrationViewModel(application: Application) : AndroidViewModel(application) {

  private val migrations =
    Services
      .serviceDirectory()
      .requireService(MigrationsType::class.java)

  private val subscriptions =
    CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val migrationEvents: UnicastWorkSubject<MigrationEvent> =
    UnicastWorkSubject.create()

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

  fun sendReport(report: MigrationReport) {
    MigrationReportEmail(report).send(getApplication())
  }
}
