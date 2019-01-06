package org.nypl.simplified.app.player

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifests
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountCredentialsHTTP
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.files.FileUtilities
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.IllegalStateException
import java.net.URI

/**
 * A fragment that downloads and updates an audio book manifest.
 */

class AudioBookLoadingFragment : Fragment() {

  companion object {

    const val parametersKey = "org.nypl.simplified.app.player.AudioBookLoadingFragment.parameters"

    /**
     * Create a new fragment.
     */

    fun newInstance(parameters: AudioBookLoadingFragmentParameters): AudioBookLoadingFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = AudioBookLoadingFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private val log = LoggerFactory.getLogger(AudioBookLoadingFragment::class.java)
  private lateinit var listener: AudioBookLoadingFragmentListenerType
  private lateinit var progress: ProgressBar
  private lateinit var services: SimplifiedCatalogAppServicesType
  private lateinit var playerParameters: AudioBookPlayerParameters
  private lateinit var loadingParameters: AudioBookLoadingFragmentParameters
  private var download: DownloadType? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {
    return inflater.inflate(R.layout.audio_book_player_loading, container, false)
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    super.onViewCreated(view, state)

    this.progress = view.findViewById(R.id.audio_book_loading_progress)
    this.progress.isIndeterminate = true
    this.progress.max = 100
    this.progress.progressTintList =
      ColorStateList.valueOf(this.loadingParameters.primaryColor)
    this.progress.indeterminateTintList =
      ColorStateList.valueOf(this.loadingParameters.primaryColor)
  }

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")

    super.onCreate(state)

    this.loadingParameters =
      this.arguments!!.getSerializable(AudioBookLoadingFragment.parametersKey)
        as AudioBookLoadingFragmentParameters
  }

  override fun onActivityCreated(state: Bundle?) {
    super.onActivityCreated(state)

    this.services = Simplified.getCatalogAppServices()!!
    this.listener = this.activity as AudioBookLoadingFragmentListenerType
    this.playerParameters = this.listener.onLoadingFragmentWantsAudioBookParameters()

    /*
     * If network connectivity is available, download a new version of the manifest. If it isn't
     * available, just use the existing one.
     */

    val fragment = this
    if (this.listener.onLoadingFragmentIsNetworkConnectivityAvailable()) {
      this.services.books.accountGetCachedLoginDetails(
        object : AccountGetCachedCredentialsListenerType {
          override fun onAccountIsNotLoggedIn() {
            fragment.listener.onLoadingFragmentLoadingFinished(
              this@AudioBookLoadingFragment.parseManifest(fragment.playerParameters.manifestFile))
          }

          override fun onAccountIsLoggedIn(credentials: AccountCredentials) {
            fragment.tryFetchNewManifest(
              credentials,
              fragment.playerParameters.manifestURI,
              fragment.listener)
          }
        })
    } else {
      this.parseAndFinishManifest()
    }
  }

  private fun tryFetchNewManifest(
    credentials: AccountCredentials,
    manifestURI: URI,
    listener: AudioBookLoadingFragmentListenerType) {

    val downloader = listener.onLoadingFragmentWantsDownloader()
    val fragment = this

    this.download =
      downloader.download(
        manifestURI,
        Option.some(AccountCredentialsHTTP.toHttpAuth(credentials)),
        object : DownloadListenerType {
          override fun onDownloadStarted(
            download: DownloadType,
            expectedTotal: Long) {
            fragment.onManifestDownloadStarted()
          }

          override fun onDownloadDataReceived(
            download: DownloadType,
            runningTotal: Long,
            expectedTotal: Long) {
            fragment.onManifestDownloadDataReceived(
              runningTotal, expectedTotal)
          }

          override fun onDownloadCancelled(download: DownloadType) {

          }

          override fun onDownloadFailed(
            download: DownloadType,
            status: Int,
            runningTotal: Long,
            exception: OptionType<Throwable>) {
            fragment.onManifestDownloadFailed(status, exception)
          }

          override fun onDownloadCompleted(
            download: DownloadType,
            file: File) {
            fragment.onManifestDownloaded(download.contentType, file)
          }
        })
  }

  private fun onManifestDownloaded(
    contentType: String,
    file: File) {

    UIThread.runOnUIThread {
      this.progress.isIndeterminate = false
      this.progress.progress = 100
    }

    /*
     * Update the manifest in the book database.
     */

    val entry =
      this.services.books.bookGetDatabase().databaseOpenExistingEntry(this.playerParameters.bookID)

    val handleOpt =
      entry.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

    if (handleOpt is Some<BookDatabaseEntryFormatHandleAudioBook>) {
      val handle = handleOpt.get()
      if (handle.formatDefinition.supportedContentTypes().contains(contentType)) {
        handle.copyInManifestAndURI(file, this.playerParameters.manifestURI)
        FileUtilities.fileDelete(file)
      } else {
        this.log.error(
          "Server delivered an unsupported content type: {}: ", contentType, IOException())
      }
    } else {
      this.log.error(
        "Bug: Book database entry has no audio book format handle", IllegalStateException())
    }

    this.parseAndFinishManifest()
  }

  private fun parseManifest(manifestFile: File): PlayerManifest {
    return FileInputStream(manifestFile).use { stream ->
      val parseResult = PlayerManifests.parse(stream)
      when (parseResult) {
        is PlayerResult.Success -> {
          parseResult.result
        }
        is PlayerResult.Failure -> {
          throw parseResult.failure
        }
      }
    }
  }

  private fun onManifestDownloadFailed(
    status: Int,
    exception: OptionType<Throwable>) {

    if (exception is Some<Throwable>) {
      this.log.error("manifest download failed: status {}: ", status, exception.get())
    } else {
      this.log.error("manifest download failed: status {}", status)
    }

    this.parseAndFinishManifest()
  }

  private fun parseAndFinishManifest() {
    try {
      val manifest = this.parseManifest(this.playerParameters.manifestFile)
      this.listener.onLoadingFragmentLoadingFinished(manifest)
    } catch (ex: Exception) {
      this.listener.onLoadingFragmentLoadingFailed(ex)
    }
  }

  private fun onManifestDownloadDataReceived(
    runningTotal: Long,
    expectedTotal: Long) {

    val progress = (runningTotal.toDouble() / expectedTotal.toDouble()) * 100.0
    UIThread.runOnUIThread {
      this.progress.isIndeterminate = false
      this.progress.progress = progress.toInt()
    }
  }

  private fun onManifestDownloadStarted() {
    UIThread.runOnUIThread {
      this.progress.progress = 0
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    this.log.debug("onDestroy")
    this.download?.cancel()
  }
}
