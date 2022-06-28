package org.nypl.simplified.viewer.audiobook.session

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.player.api.PlayerAudioEngines
import org.librarysimplified.audiobook.player.api.PlayerBookID
import org.librarysimplified.audiobook.player.api.PlayerUserAgent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.audio.AudioBookManifestStrategyType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.viewer.audiobook.R
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerContract
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerParameters
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerSessionHolder
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Try
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI

internal class AudioBookPlayerSessionBuilder(
  private val application: Application,
  private val profilesController: ProfilesControllerType,
  private val strategies: AudioBookManifestStrategiesType,
  private val networkConnectivity: NetworkConnectivityType,
  private val cacheDirectory: File,
  feedbooksConfigService: AudioBookFeedbooksSecretServiceType?
) {

  private val logger =
    LoggerFactory.getLogger(AudioBookPlayerSessionBuilder::class.java)

  private val readiumAdapter: AudioBookManifestReadiumAdapter =
    AudioBookManifestReadiumAdapter(feedbooksConfigService?.configuration)

  suspend fun open(
    parameters: AudioBookPlayerParameters
  ): Try<AudioBookPlayerSessionHolder, Exception> {
    return try {
        val sessionHolder = openThrowing(parameters)
        Try.success(sessionHolder)
    } catch (e: Exception) {
      Try.failure(e)
    }
  }

  @OptIn(ExperimentalMedia2::class)
  private suspend fun openThrowing(
    parameters: AudioBookPlayerParameters
  ): AudioBookPlayerSessionHolder {
    val account =
      profilesController.profileAccountForBook(parameters.bookID)

    val credentials =
      account
        .loginState
        .credentials

    val formatHandle =
      account
        .bookDatabase
        .entry(parameters.bookID)
        .findFormatHandle(BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook::class.java)
        ?: throw Exception("Couldn't find a handle for the book.")

    val strategy =
      parameters.toManifestStrategy(
        strategies = strategies,
        isNetworkAvailable = { networkConnectivity.isNetworkAvailable },
        credentials = credentials,
        cacheDirectory = cacheDirectory
      )

    val playerManifest =
      getManifest(parameters, strategy)
        .getOrThrow()

    val publication =
      readiumAdapter.createPublication(playerManifest, strategy, parameters.opdsEntry)

    val bookId =
      PlayerBookID.transform(playerManifest.metadata.identifier)

    val playerFactory = PlayerAudioEngines.findBestFor(
      PlayerAudioEngineRequest(
        context = application,
        bookID = bookId,
        publication = publication,
        manifest = playerManifest,
        downloadManifest = { (strategy.execute() as TaskResult.Success<AudioBookManifestData>).result.manifest },
        userAgent = PlayerUserAgent(parameters.userAgent),
        filter = { true }
      )
    )

    if (playerFactory == null) {
      val title =
        this.application.resources.getString(R.string.audio_book_player_error_engine_open)
      throw IllegalStateException(title)
    }

    val player = playerFactory.createPlayer()

    val initialLocator =
      formatHandle.format.position
        ?.let {
          val currentLink = publication.readingOrder[it.chapter] //FIXME: what about part?
          Locator(
            href = currentLink.href,
            type = currentLink.type.orEmpty(),
            locations = Locator.Locations(fragments = listOf("t=${it.offsetMilliseconds}"))
          )
        }

    logger.debug("Initial locator is $initialLocator.")


    val navigator = MediaNavigator.create(
      context = application,
      publication = publication,
      initialLocator = initialLocator,
      player = player.sessionPlayer
    ).getOrThrow()

    val activityIntent = createSessionActivityIntent(parameters)
    val mediaSession = navigator.session(application, activityIntent)

    return AudioBookPlayerSessionHolder(
      parameters = parameters,
      navigator = navigator,
      mediaSession = mediaSession,
      player = player,
      formatHandle = formatHandle
    )
  }


  private suspend fun getManifest(
    parameters: AudioBookPlayerParameters,
    strategy: AudioBookManifestStrategyType
  ): Try<PlayerManifest, Exception> = withContext(Dispatchers.IO){
    val manifestData = when (val strategyResult = strategy.execute()) {
      is TaskResult.Success -> {
        saveManifest(
          profiles = profilesController,
          bookId = parameters.bookID,
          manifestURI = parameters.manifestURI,
          manifest = strategyResult.result.fulfilled
        )
        strategyResult.result
      }
      is TaskResult.Failure -> {
        val exception = IOException(strategyResult.message)
        return@withContext Try.failure(exception)
      }
    }

    return@withContext Try.success(manifestData.manifest)
  }

  /**
   * Attempt to save a manifest in the books database.
   */

  private suspend fun saveManifest(
    profiles: ProfilesControllerType,
    bookId: BookID,
    manifestURI: URI,
    manifest: ManifestFulfilled
  ) = withContext(Dispatchers.IO) {
    val handle =
      profiles.profileAccountForBook(bookId)
        .bookDatabase
        .entry(bookId)
        .findFormatHandle(BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook::class.java)

    val contentType = manifest.contentType
    if (handle == null) {
      logger.error(
        "Bug: Book database entry has no audio book format handle", IllegalStateException()
      )
      return@withContext
    }

    if (!handle.formatDefinition.supports(contentType)) {
      logger.error(
        "Server delivered an unsupported content type: {}: ", contentType, IOException()
      )
      return@withContext
    }
    handle.copyInManifestAndURI(manifest.data, manifestURI)
  }

  private fun createSessionActivityIntent(audioBookPlayerParameters: AudioBookPlayerParameters): PendingIntent {
    // This intent will be triggered when the notification is clicked.
    var flags = PendingIntent.FLAG_UPDATE_CURRENT
    flags = flags or PendingIntent.FLAG_IMMUTABLE

    val intent = AudioBookPlayerContract.createIntent(application, audioBookPlayerParameters)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    return PendingIntent.getActivity(application, 0, intent, flags)
  }
}
