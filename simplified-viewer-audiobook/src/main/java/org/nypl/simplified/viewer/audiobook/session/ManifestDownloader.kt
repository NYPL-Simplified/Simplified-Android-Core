package org.nypl.simplified.viewer.audiobook.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.irradia.mime.vanilla.MIMEParser
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.audio.AudioBookCredentials
import org.nypl.simplified.books.audio.AudioBookManifestRequest
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.audio.AudioBookManifestStrategyType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerParameters
import org.readium.r2.shared.util.Try
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI

internal class ManifestDownloader(
  private val profilesController: ProfilesControllerType,
  private val strategies: AudioBookManifestStrategiesType,
  private val networkConnectivity: NetworkConnectivityType,
  private val cacheDirectory: File,
) {
  private val logger =
    LoggerFactory.getLogger(ManifestDownloader::class.java)

  suspend fun downloadManifest(
    parameters: AudioBookPlayerParameters,
    credentials: AccountAuthenticationCredentials?,
  ): Try<PlayerManifest, Exception> {
    val strategy =
      createManifestStrategy(
        parameters, strategies,
        { networkConnectivity.isNetworkAvailable },
        credentials, cacheDirectory
      )

    return downloadManifest(parameters, strategy)
  }

  private suspend fun downloadManifest(
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

  /**
   * Create a manifest strategy for the current parameters.
   */

  private fun createManifestStrategy(
    parameters: AudioBookPlayerParameters,
    strategies: AudioBookManifestStrategiesType,
    isNetworkAvailable: () -> Boolean,
    credentials: AccountAuthenticationCredentials?,
    cacheDirectory: File
  ): AudioBookManifestStrategyType {
    val manifestContentType =
      MIMEParser.parseRaisingException(parameters.manifestContentType)
    val userAgent =
      PlayerUserAgent(parameters.userAgent)

    val audioBookCredentials =
      when (credentials) {
        is AccountAuthenticationCredentials.Basic -> {
          if (credentials.password.value.isBlank()) {
            AudioBookCredentials.UsernameOnly(
              userName = credentials.userName.value
            )
          } else {
            AudioBookCredentials.UsernamePassword(
              userName = credentials.userName.value,
              password = credentials.password.value
            )
          }
        }
        is AccountAuthenticationCredentials.OAuthWithIntermediary ->
          AudioBookCredentials.BearerToken(credentials.accessToken)
        is AccountAuthenticationCredentials.SAML2_0 ->
          AudioBookCredentials.BearerToken(credentials.accessToken)
        null ->
          null
      }

    val request =
      AudioBookManifestRequest(
        targetURI = parameters.manifestURI,
        contentType = manifestContentType,
        userAgent = userAgent,
        credentials = audioBookCredentials,
        services = Services.serviceDirectory(),
        isNetworkAvailable = isNetworkAvailable,
        loadFallbackData = {
          ManifestFulfilled(manifestContentType, parameters.manifestFile.readBytes())
        },
        cacheDirectory = cacheDirectory
      )

    return strategies.createStrategy(request)
  }
}
