package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.migration.spi.MigrationEvent

class MigrationProgressFragment : Fragment(R.layout.splash_migration_progress) {

  private lateinit var progressBar: ProgressBar
  private lateinit var textView: TextView

  private val subscriptions = CompositeDisposable()
  private val viewModel: MigrationViewModel by viewModels(
    ownerProducer = this::requireParentFragment
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.startMigrationsIfNotStarted()
    viewModel.migrationReport.observe(this) {
      setFragmentResult("", Bundle())
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    textView = view.findViewById(R.id.splashMigrationProgressText)
    progressBar = view.findViewById(R.id.splashMigrationProgress)
  }

  override fun onStart() {
    super.onStart()
    viewModel.migrationEvents
      .subscribe(this::onMigrationEvent)
      .let { subscriptions.add(it) }
  }

  override fun onStop() {
    super.onStop()
    subscriptions.clear()
  }

  private fun onMigrationEvent(event: MigrationEvent) {
    textView.text = event.message
  }
}
