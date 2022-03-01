package org.nypl.simplified.ui.splash

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.slf4j.LoggerFactory

class SplashFragment : Fragment(R.layout.splash_fragment), FragmentResultListener {

  private val logger = LoggerFactory.getLogger(SplashFragment::class.java)
  private val listener: FragmentListenerType<SplashEvent> by fragmentListeners()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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

    this.listener.post(SplashEvent.SplashCompleted)
  }

  private fun showEula() {
    this.logger.debug("showEula")
    childFragmentManager.commit {
      replace(R.id.splash_fragment_container, EulaFragment::class.java, Bundle())
    }
  }
}
