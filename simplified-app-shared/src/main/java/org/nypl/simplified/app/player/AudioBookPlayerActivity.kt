package org.nypl.simplified.app.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.ImageView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerAudioEngineRequest
import org.nypl.audiobook.android.api.PlayerAudioEngines
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.api.PlayerSleepTimer
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.downloads.DownloadProvider
import org.nypl.audiobook.android.views.PlayerFragment
import org.nypl.audiobook.android.views.PlayerFragmentListenerType
import org.nypl.audiobook.android.views.PlayerFragmentParameters
import org.nypl.audiobook.android.views.PlayerPlaybackRateFragment
import org.nypl.audiobook.android.views.PlayerSleepTimerFragment
import org.nypl.audiobook.android.views.PlayerTOCFragment
import org.nypl.audiobook.android.views.PlayerTOCFragmentParameters
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType
import org.nypl.simplified.app.utilities.NamedThreadPools
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * The main activity for playing audio books.
 */

class AudioBookPlayerActivity : FragmentActivity(),
  AudioBookLoadingFragmentListenerType, PlayerFragmentListenerType {

  private val log: Logger = LoggerFactory.getLogger(AudioBookPlayerActivity::class.java)

  companion object {

    private const val PARAMETER_ID =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.parameters"

    /**
     * Start a new player for the given book.
     *
     * @param from The parent activity
     * @param parameters The player parameters
     */

    fun startActivity(
      from: Activity,
      parameters: AudioBookPlayerParameters) {

      val b = Bundle()
      b.putSerializable(this.PARAMETER_ID, parameters)
      val i = Intent(from, AudioBookPlayerActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  private lateinit var book: PlayerAudioBookType
  private lateinit var bookTitle: String
  private lateinit var bookAuthor: String
  private lateinit var player: PlayerType
  private var playerInitialized: Boolean = false
  private lateinit var playerFragment: PlayerFragment
  private lateinit var parameters: AudioBookPlayerParameters
  private lateinit var services: SimplifiedCatalogAppServicesType
  private lateinit var loadingFragment: AudioBookLoadingFragment
  private lateinit var sleepTimer: PlayerSleepTimerType
  private lateinit var downloadProvider: PlayerDownloadProviderType
  private lateinit var downloadExecutor: ListeningExecutorService
  private lateinit var downloaderDir: File
  private lateinit var downloader: DownloaderType
  private var download: DownloadType? = null

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.log.debug("onCreate")

    val i = this.intent!!
    val a = i.extras

    this.parameters = a.getSerializable(PARAMETER_ID) as AudioBookPlayerParameters

    this.log.debug("manifest file: {}", this.parameters.manifestFile)
    this.log.debug("manifest uri:  {}", this.parameters.manifestURI)
    this.log.debug("book id:       {}", this.parameters.bookID)
    this.log.debug("entry id:      {}", this.parameters.opdsEntry.id)

    this.setContentView(R.layout.audio_book_player_base)
    this.services = Simplified.getCatalogAppServices()!!

    this.bookTitle = this.parameters.opdsEntry.title
    this.bookAuthor = findBookAuthor(this.parameters.opdsEntry)

    /*
     * Create a new downloader that is solely used to fetch audio book manifests.
     */

    this.downloadExecutor =
      MoreExecutors.listeningDecorator(
        NamedThreadPools.namedThreadPool(4, "audiobook-player", 19))

    this.downloaderDir =
      File(this.filesDir, "audiobook-player-downloads")
    DirectoryUtilities.directoryCreate(this.downloaderDir)
    this.downloader =
      DownloaderHTTP.newDownloader(this.downloadExecutor, this.downloaderDir, this.services.http)
    this.downloadProvider =
      DownloadProvider.create(this.downloadExecutor)

    /*
     * Create a sleep timer.
     */

    this.sleepTimer = PlayerSleepTimer.create()

    /*
     * Show a loading fragment.
     */

    this.loadingFragment = AudioBookLoadingFragment.newInstance()
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.audio_book_player_fragment_holder, this.loadingFragment, "LOADING")
      .commit()
  }

  private fun findBookAuthor(entry: OPDSAcquisitionFeedEntry): String {
    if (entry.authors.isEmpty()) {
      return ""
    }
    return entry.authors.first()
  }

  override fun onDestroy() {
    super.onDestroy()
    this.log.debug("onDestroy")

    val down = this.download
    if (down != null) {
      down.cancel()
    }

    this.downloadExecutor.shutdown()

    if (this.playerInitialized) {
      this.player.close()
    }
  }

  override fun onLoadingFragmentWantsDownloader(): DownloaderType {
    return this.downloader
  }

  override fun onLoadingFragmentIsNetworkConnectivityAvailable(): Boolean {
    return this.services.isNetworkAvailable
  }

  override fun onLoadingFragmentWantsAudioBookParameters(): AudioBookPlayerParameters {
    return this.parameters
  }

  override fun onLoadingFragmentFinishedLoading(manifest: PlayerManifest) {
    this.log.debug("finished loading")

    /*
     * Ask the API for the best audio engine available that can handle the given manifest.
     */

    val engine = PlayerAudioEngines.findBestFor(
      PlayerAudioEngineRequest(
        manifest = manifest,
        filter = { true },
        downloadProvider = DownloadProvider.create(this.downloadExecutor)))

    if (engine == null) {
      throw UnimplementedCodeException()
    }

    this.log.debug(
      "selected audio engine: {} {}",
      engine.engineProvider.name(),
      engine.engineProvider.version())

    /*
     * Create the audio book.
     */

    val bookResult = engine.bookProvider.create(this)
    if (bookResult is PlayerResult.Failure) {
      throw UnimplementedCodeException()
    }

    this.book = (bookResult as PlayerResult.Success).result
    this.player = book.createPlayer()
    this.playerInitialized = true

    /*
     * Create and load the main player fragment into the holder view declared in the activity.
     */

    UIThread.runOnUIThread {
      this.playerFragment = PlayerFragment.newInstance(PlayerFragmentParameters())

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.audio_book_player_fragment_holder, this.playerFragment, "PLAYER")
        .commit()
    }
  }

  override fun onPlayerPlaybackRateShouldOpen() {

    /*
     * The player fragment wants us to open the playback rate selection dialog.
     */

    UIThread.runOnUIThread {
      val fragment = PlayerPlaybackRateFragment.newInstance()
      fragment.show(this.supportFragmentManager, "PLAYER_RATE")
    }
  }

  override fun onPlayerSleepTimerShouldOpen() {

    /*
     * The player fragment wants us to open the sleep timer.
     */

    UIThread.runOnUIThread {
      val fragment = PlayerSleepTimerFragment.newInstance()
      fragment.show(this.supportFragmentManager, "PLAYER_SLEEP_TIMER")
    }
  }

  override fun onPlayerTOCShouldOpen() {

    /*
     * The player fragment wants us to open the table of contents. Load and display it, and
     * also set the action bar title.
     */

    UIThread.runOnUIThread {
      this.actionBar.setTitle(R.string.audiobook_player_toc_title)

      val fragment =
        PlayerTOCFragment.newInstance(PlayerTOCFragmentParameters())

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.audio_book_player_fragment_holder, fragment, "PLAYER_TOC")
        .addToBackStack(null)
        .commit()
    }
  }

  override fun onPlayerTOCWantsBook(): PlayerAudioBookType {
    return this.book
  }

  override fun onPlayerTOCWantsClose() {

    /*
     * The player fragment wants to close the table of contents dialog. Pop it from the back
     * stack and set the action bar title back to the original title.
     */

    this.supportFragmentManager.popBackStack()
    this.actionBar.setTitle(R.string.audio_book_player)
  }

  override fun onPlayerWantsAuthor(): String {
    return this.bookAuthor
  }

  override fun onPlayerWantsCoverImage(view: ImageView) {

  }

  override fun onPlayerWantsPlayer(): PlayerType {
    return this.player
  }

  override fun onPlayerWantsSleepTimer(): PlayerSleepTimerType {
    return this.sleepTimer
  }

  override fun onPlayerWantsTitle(): String {
    return this.parameters.opdsEntry.title
  }
}
