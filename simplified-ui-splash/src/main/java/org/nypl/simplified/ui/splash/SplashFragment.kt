package org.nypl.simplified.ui.splash

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory

class SplashFragment : Fragment(R.layout.splash_fragment) {

  private val logger = LoggerFactory.getLogger(SplashFragment::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    childFragmentManager.setFragmentResultListener("", this) { _, _ ->
      when (childFragmentManager.fragments.last()) {
        is BootFragment -> onBootCompleted()
        is EulaFragment -> onEulaFinished()
        is MigrationProgressFragment -> onMigrationCompleted()
        is MigrationReportFragment -> onMigrationReportFinished()
      }
    }
  }

  private fun onBootCompleted() {
    selectAnonymousProfile()

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
    showMigrationReport()
  }

  private fun onMigrationReportFinished() {
    requireActivity().supportFragmentManager.setFragmentResult("", Bundle())
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

  private fun selectAnonymousProfile() {
    val profilesController =
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    val profileMode =
      profilesController
        .profileAnonymousEnabled()

    when (profileMode) {
      ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED -> {
        profilesController.profileSelect(profilesController.profileCurrent().id)
      }
      ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED -> {
      }
    }
  }
}
