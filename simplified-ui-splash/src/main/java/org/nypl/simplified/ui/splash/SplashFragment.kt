package org.nypl.simplified.ui.splash

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.services.api.Services
import org.slf4j.LoggerFactory

class SplashFragment : Fragment(R.layout.splash_fragment), FragmentResultListener {

  companion object {

    private const val resultKeyKey = "org.nypl.simplified.splash.result.key"

    fun newInstance(resultKey: String) = SplashFragment().apply {
      arguments = bundleOf(resultKeyKey to resultKey)
    }
  }

  private lateinit var resultKey: String

  private val logger = LoggerFactory.getLogger(SplashFragment::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    resultKey =
      requireNotNull(requireArguments().getString(resultKeyKey))

    childFragmentManager.setFragmentResultListener(
      "",
      this,
      this::onFragmentResult
    )
  }

  override fun onFragmentResult(requestKey: String, result: Bundle) {
    when (childFragmentManager.fragments.last()) {
      is BootFragment -> onBootCompleted()
      is EulaFragment -> onEulaFinished()
      is MigrationProgressFragment -> onMigrationCompleted()
      is MigrationReportFragment -> onMigrationReportFinished()
    }
  }

  private fun onBootCompleted() {
    val eula =
      Services.serviceDirectory()
        .requireService(DocumentStoreType::class.java)
        .eula

    if (eula != null && !eula.hasAgreed) {
      showEula()
    } else {
      onEulaFinished()
    }
  }

  private fun onEulaFinished() {
    val eula =
      Services.serviceDirectory()
        .requireService(DocumentStoreType::class.java)
        .eula

    if (eula != null && !eula.hasAgreed) {
      this.logger.error("eula declined: terminating")
      requireActivity().finish()
    }

    val migrationViewModel =
      ViewModelProvider(this)
        .get(MigrationViewModel::class.java)

    if (migrationViewModel.anyMigrationNeedToRun()) {
      showMigrationRunning()
    } else {
      this.logger.debug("no migration to run")
      onMigrationReportFinished()
    }
  }

  private fun onMigrationCompleted() {
    val migrationViewModel =
      ViewModelProvider(this)
        .get(MigrationViewModel::class.java)

    if (migrationViewModel.migrationReport.value != null) {
      showMigrationReport()
    } else {
      this.logger.debug("no report to show")
      onMigrationReportFinished()
    }
  }

  private fun onMigrationReportFinished() {
    requireActivity().supportFragmentManager.setFragmentResult(resultKey, Bundle())
  }

  private fun showEula() {
    this.logger.debug("showEula")
    childFragmentManager.commit {
      replace(R.id.splash_fragment_container, EulaFragment::class.java, Bundle())
    }
  }

  private fun showMigrationRunning() {
    this.logger.debug("showMigrationRunning")
    childFragmentManager.commit {
      replace(R.id.splash_fragment_container, MigrationProgressFragment::class.java, Bundle())
    }
  }

  private fun showMigrationReport() {
    this.logger.debug("showMigrationReport")
    childFragmentManager.commit {
      replace(R.id.splash_fragment_container, MigrationReportFragment::class.java, Bundle())
    }
  }
}
