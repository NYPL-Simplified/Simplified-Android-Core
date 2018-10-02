package org.nypl.simplified.app.player

import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.simplified.downloader.core.DownloaderType

/**
 * The interface that must be implemented by activities hosting a {@link AudioBookLoadingFragment}.
 */

interface AudioBookLoadingFragmentListenerType {

  /**
   * @return A downloader for the fragment
   */

  fun onLoadingFragmentWantsDownloader(): DownloaderType

  /**
   * @return `true` if network connectivity is currently available
   */

  fun onLoadingFragmentIsNetworkConnectivityAvailable(): Boolean

  /**
   * @return The parameters that were used to instantiate the audio book player
   */

  fun onLoadingFragmentWantsAudioBookParameters(): AudioBookPlayerParameters

  /**
   * Called when the loading and parsing of the manifest has finished.
   */

  fun onLoadingFragmentFinishedLoading(manifest: PlayerManifest)

}
