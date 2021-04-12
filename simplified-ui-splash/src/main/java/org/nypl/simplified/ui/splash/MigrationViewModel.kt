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

  init {
    migrations.events
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onMigrationEvent)
      .let { subscriptions.add(it) }
  }

  private fun onMigrationEvent(event: MigrationEvent) {
    migrationEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val migrationEvents: UnicastWorkSubject<MigrationEvent> =
    UnicastWorkSubject.create()

  val migrationReport: MutableLiveData<MigrationReport?> =
    MutableLiveData()

  fun startMigrationsIfNotStarted(): Future<MigrationReport?> {
    val future = migrations.start()

    future.addListener(
      { migrationReport.postValue(future.get()) },
      MoreExecutors.directExecutor()
    )

    return future
  }

  fun anyMigrationNeedToRun(): Boolean {
    return migrations.anyNeedToRun()
  }

  fun sendReport() {
    val report = checkNotNull(migrationReport.value)
    MigrationReportEmail
      .fromMigrationReport(report)
      .send(getApplication())
  }
}
