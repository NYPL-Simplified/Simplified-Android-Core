package org.nypl.simplified.viewer.audiobook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListeningExecutorService
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * A fragment that downloads and updates an audio book manifest.
 */

class AudioBookLoadingFragment : Fragment() {

  companion object {

    const val parametersKey =
      "org.nypl.simplified.viewer.audiobook.AudioBookLoadingFragment.parameters"

    /**
     * Create a new fragment.
     */

    fun newInstance(parameters: AudioBookLoadingFragmentParameters): AudioBookLoadingFragment {
      val args = Bundle()
      args.putSerializable(parametersKey, parameters)
      val fragment = AudioBookLoadingFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var ioExecutor: ListeningExecutorService
  private lateinit var listener: AudioBookLoadingFragmentListenerType
  private lateinit var loadingParameters: AudioBookLoadingFragmentParameters
  private lateinit var playerParameters: AudioBookPlayerParameters
  private lateinit var profiles: ProfilesControllerType
  private lateinit var progress: ProgressBar
  private lateinit var strategies: AudioBookManifestStrategiesType
  private lateinit var uiThread: UIThreadServiceType
  private val log = LoggerFactory.getLogger(AudioBookLoadingFragment::class.java)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.audio_book_player_loading, container, false)
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    super.onViewCreated(view, state)

    this.progress = view.findViewById(R.id.audio_book_loading_progress)
    this.progress.isIndeterminate = true
    this.progress.max = 100
  }

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")

    super.onCreate(state)

    this.loadingParameters =
      this.arguments!!.getSerializable(parametersKey)
      as AudioBookLoadingFragmentParameters

    val services = Services.serviceDirectory()

    this.profiles =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.strategies =
      services.requireService(AudioBookManifestStrategiesType::class.java)
  }

  override fun onActivityCreated(state: Bundle?) {
    super.onActivityCreated(state)

    this.listener = this.activity as AudioBookLoadingFragmentListenerType

    this.playerParameters =
      this.listener.onLoadingFragmentWantsAudioBookParameters()
    this.ioExecutor =
      this.listener.onLoadingFragmentWantsIOExecutor()

    val credentials =
      this.profiles.profileAccountForBook(this.playerParameters.bookID)
        .loginState
        .credentials

    this.ioExecutor.execute {
      try {
        this.uiThread.runOnUIThread {
          this.progress.isIndeterminate = true
          this.progress.progress = 0
        }

        val manifest = this.downloadAndSaveManifest(credentials)

        this.uiThread.runOnUIThread {
          this.progress.isIndeterminate = false
          this.progress.progress = 100
        }

        this.listener.onLoadingFragmentLoadingFinished(manifest)
      } catch (e: Exception) {
        this.uiThread.runOnUIThread {
          this.progress.isIndeterminate = false
          this.progress.progress = 100
        }

        this.listener.onLoadingFragmentLoadingFailed(e)
      }
    }
  }

  private fun downloadAndSaveManifest(
    credentials: AccountAuthenticationCredentials?
  ): PlayerManifest {
    val strategy =
      this.playerParameters.toManifestStrategy(
        this.strategies,
        this.listener::onLoadingFragmentIsNetworkConnectivityAvailable,
        credentials,
        this.requireContext().cacheDir
      )
    return when (val strategyResult = strategy.execute()) {
      is TaskResult.Success -> {
        AudioBookHelpers.saveManifest(
          profiles = this.profiles,
          bookId = this.playerParameters.bookID,
          manifestURI = this.playerParameters.manifestURI,
          manifest = strategyResult.result.fulfilled
        )
        strategyResult.result.manifest
      }
      is TaskResult.Failure ->
        throw IOException(strategyResult.message)
    }
  }
}
