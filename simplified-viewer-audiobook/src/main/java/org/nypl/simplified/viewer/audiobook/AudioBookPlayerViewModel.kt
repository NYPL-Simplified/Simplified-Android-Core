package org.nypl.simplified.viewer.audiobook

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.librarysimplified.audiobook.player.api.PlayerBookID
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.player.api.PlayerAudioEngines
import org.librarysimplified.audiobook.player.api.PlayerUserAgent
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenState
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.util.Try
import org.slf4j.LoggerFactory
import java.io.IOException

@OptIn(ExperimentalMedia2::class, ExperimentalCoroutinesApi::class)
internal class AudioBookPlayerViewModel private constructor(
  private val application: Application,
  private val parameters: AudioBookPlayerParameters,
  private val strategies: AudioBookManifestStrategiesType,
  private val networkConnectivity: NetworkConnectivityType,
  private val profilesController: ProfilesControllerType,
  private val covers: BookCoverProviderType,
  private val feedbooksConfigService: AudioBookFeedbooksSecretServiceType?
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(AudioBookPlayerViewModel::class.java)

  private val serviceBinder: CompletableDeferred<AudioBookPlayerService.Binder> =
    CompletableDeferred()

  init {
    val mediaServiceConnection = object : ServiceConnection {

      override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        logger.debug("MediaService bound.")
        serviceBinder.complete(service as AudioBookPlayerService.Binder)
      }

      override fun onServiceDisconnected(name: ComponentName) {
        logger.debug("MediaService disconnected.")
        // Should not happen, do nothing.
      }

      override fun onNullBinding(name: ComponentName) {
        logger.debug("Failed to bind to MediaService.")
        // Should not happen, do nothing.
      }
    }

    // AudioBookPlayerService.onBind requires the intent to have a non-null action.
    val intent = Intent(AudioBookPlayerService.SERVICE_INTERFACE)
      .apply { setClass(application, AudioBookPlayerService::class.java) }
    application.startService(intent)
    application.bindService(intent, mediaServiceConnection, 0)
  }

  private val readiumAdapter: AudioBookManifestReadiumAdapter =
    AudioBookManifestReadiumAdapter(feedbooksConfigService?.configuration)

  private suspend fun getSession(): Try<AudioBookPlayerSessionHolder, Exception> {
    val binder = serviceBinder.await()

    binder.sessionHolder
      ?.takeIf { (it.parameters.bookID == parameters.bookID) }
      ?.let { return Try.success(it) }

    binder.sessionHolder?.close()

    return openSession()
  }

  private suspend fun openSession(): Try<AudioBookPlayerSessionHolder, Exception> {

    val account =
      this.profilesController.profileAccountForBook(this.parameters.bookID)

    val credentials =
      account
        .loginState
        .credentials

    val formatHandle =
      account
        .bookDatabase
        .entry(this.parameters.bookID)
        .findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)
        ?: return Try.failure(Exception("Couldn't find a handle for the book."))

    val strategy =
      this.parameters.toManifestStrategy(
        strategies = this.strategies,
        isNetworkAvailable = { this.networkConnectivity.isNetworkAvailable },
        credentials = credentials,
        cacheDirectory = application.cacheDir
      )

    val manifestData = when (val strategyResult = strategy.execute()) {
      is TaskResult.Success -> {
        AudioBookHelpers.saveManifest(
          profiles = this.profilesController,
          bookId = this.parameters.bookID,
          manifestURI = this.parameters.manifestURI,
          manifest = strategyResult.result.fulfilled
        )
        strategyResult.result
      }
      is TaskResult.Failure -> {
        val exception = IOException(strategyResult.message)
        return Try.failure(exception)
      }
    }

    val playerManifest = manifestData.manifest

    val publication =
      readiumAdapter.createPublication(playerManifest, strategy)

    val bookId =
      PlayerBookID.transform(playerManifest.metadata.identifier)

    val playerFactory = PlayerAudioEngines.findBestFor(
      PlayerAudioEngineRequest(
        context = application,
        bookID = bookId,
        publication = publication,
        manifest = playerManifest,
        downloadManifest = { (strategy.execute() as TaskResult.Success<AudioBookManifestData>).result.manifest },
        userAgent = PlayerUserAgent(this.parameters.userAgent),
        filter = { true }
      )
    )

    if (playerFactory == null) {
      val title =
        this.application.resources.getString(R.string.audio_book_player_error_engine_open)
      return Try.failure(IllegalStateException(title))
    }

    val player = playerFactory.createPlayer()

    val initialLocator =
      formatHandle.format.position
        ?.let {
          val currentLink = publication.readingOrder[it.chapter] //FIXME: what about part?
          publication.locatorFromLink(currentLink)
            ?.copyWithLocations(fragments = listOf("t=${it.offsetMilliseconds}"))
        }


    val navigator =  MediaNavigator.create(
      context = application,
      publication = publication,
      initialLocator = initialLocator,
      player = player.sessionPlayer
    )

    return navigator.map {
      serviceBinder.await().bindNavigator(it, player, parameters, formatHandle)
    }
  }

  private val listenerDelegate = AudioBookPlayerListener()

  val currentScreen: State<AudioBookPlayerListener.Screen>
    get() = listenerDelegate.currentScreen

  fun popBackstack() {
    listenerDelegate.popBackstack()
  }

  init {
      viewModelScope.launch {
        getSession().fold(
          onSuccess = { session ->
            listenerDelegate.openPlayer(
              PlayerScreenState(session.navigator, parameters.opdsEntry, viewModelScope)
            )
          },
          onFailure = {
            listenerDelegate.openError(it)
          }
        )
      }
  }

  fun closeNavigator() {
    if (serviceBinder.isCompleted)
    serviceBinder.getCompleted().closeNavigator()
  }

  internal class Factory(
    private val application: Application,
    private val parameters: AudioBookPlayerParameters,
    services: ServiceDirectoryType
  ) : ViewModelProvider.NewInstanceFactory() {

    private val strategies =
      services.requireService(AudioBookManifestStrategiesType::class.java)
    private val networkConnectivity =
      services.requireService(NetworkConnectivityType::class.java)
    private val profiles =
      services.requireService(ProfilesControllerType::class.java)
    private val covers =
        services.requireService(BookCoverProviderType::class.java)
    private val feedbooksConfigService =
      services.optionalService(AudioBookFeedbooksSecretServiceType::class.java)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return when {
        modelClass.isAssignableFrom(AudioBookPlayerViewModel::class.java) ->
          AudioBookPlayerViewModel(
            application, parameters, strategies,
            networkConnectivity, profiles, covers, feedbooksConfigService
          ) as T
        else ->
          super.create(modelClass)
      }
    }
  }
}
