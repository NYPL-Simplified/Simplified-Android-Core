package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport

class MigrationReportFragment : Fragment(R.layout.splash_migration_report) {

  private lateinit var title: TextView
  private lateinit var list: RecyclerView
  private lateinit var sendButton: Button
  private lateinit var okButton: Button

  private val viewModel: MigrationViewModel by viewModels(
    ownerProducer = this::requireParentFragment
  )
  private val report: MigrationReport by lazy {
    checkNotNull(viewModel.startMigrations().get())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    title = view.findViewById(R.id.splashMigrationReportTitle)
    list = view.findViewById(R.id.splashMigrationReportList)
    sendButton = view.findViewById(R.id.splashMigrationReportSend)
    okButton = view.findViewById(R.id.splashMigrationReportOK)

    val eventsToShow =
      report.events.filterNot { e -> e is MigrationEvent.MigrationStepInProgress }

    list.apply {
      adapter = MigrationReportListAdapter(eventsToShow)
      setHasFixedSize(false)
      layoutManager = LinearLayoutManager(this.context)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      adapter!!.notifyDataSetChanged()
    }

    val failure =
      report.events.any { e -> e is MigrationEvent.MigrationStepError }

    title.setText(
      if (failure) {
        R.string.migrationFailure
      } else {
        R.string.migrationSuccess
      }
    )
  }

  override fun onResume() {
    super.onResume()
    sendButton.setOnClickListener {
      viewModel.sendReport(report)
    }

    okButton.setOnClickListener {
      setFragmentResult("", Bundle())
    }
  }
}
