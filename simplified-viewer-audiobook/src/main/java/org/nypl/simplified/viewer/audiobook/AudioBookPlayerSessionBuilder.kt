package org.nypl.simplified.viewer.audiobook

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.media2.session.MediaSession
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.player.api.PlayerAudioEngines
import org.librarysimplified.audiobook.player.api.PlayerBookID
import org.librarysimplified.audiobook.player.api.PlayerType
import org.librarysimplified.audiobook.player.api.PlayerUserAgent
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.IOException

internal class AudioBookPlayerSessionBuilder(
  private val application: Application,
  private val profilesController: ProfilesControllerType,
  private val strategies: AudioBookManifestStrategiesType,
  private val networkConnectivity: NetworkConnectivityType,
  private val cacheDirectory: File,
  feedbooksConfigService: AudioBookFeedbooksSecretServiceType?
) {

  @OptIn(ExperimentalMedia2::class)
  data class SessionHolder(
    val parameters: AudioBookPlayerParameters,
    val navigator: MediaNavigator,
    val mediaSession: MediaSession,
    val player: PlayerType,
    val formatHandle: BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
  ) {

    fun close() {
      mediaSession.close()
      navigator.close()
      navigator.publication.close()
      player.close()
    }
  }

  private val readiumAdapter: AudioBookManifestReadiumAdapter =
    AudioBookManifestReadiumAdapter(feedbooksConfigService?.configuration)

  @OptIn(ExperimentalMedia2::class)
  suspend fun open(parameters: AudioBookPlayerParameters): Try<SessionHolder, Exception> {

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
        ?: return Try.failure(Exception("Couldn't find a handle for the book."))

    val strategy =
      parameters.toManifestStrategy(
        strategies = strategies,
        isNetworkAvailable = { networkConnectivity.isNetworkAvailable },
        credentials = credentials,
        cacheDirectory = cacheDirectory
      )

    val manifestData = when (val strategyResult = strategy.execute()) {
      is TaskResult.Success -> {
        AudioBookHelpers.saveManifest(
          profiles = profilesController,
          bookId = parameters.bookID,
          manifestURI = parameters.manifestURI,
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


    val navigatorResult =  MediaNavigator.create(
      context = application,
      publication = publication,
      initialLocator = initialLocator,
      player = player.sessionPlayer
    )

    val sessionResult = navigatorResult.map { navigator ->
      val activityIntent = createSessionActivityIntent(parameters)
      val mediaSession = navigator.session(application, activityIntent)

      SessionHolder(
        parameters = parameters,
        navigator = navigator,
        mediaSession = mediaSession,
        player = player,
        formatHandle = formatHandle
      )
    }

    return sessionResult
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
