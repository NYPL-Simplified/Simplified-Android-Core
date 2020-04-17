package org.nypl.simplified.books.audio

import com.io7m.junreachable.UnimplementedCodeException
import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.license_check.api.LicenseCheckParameters
import org.librarysimplified.audiobook.license_check.api.LicenseChecks
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicCredentials
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicParameters
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicType
import org.librarysimplified.audiobook.manifest_fulfill.opa.OPAManifestFulfillmentStrategyProviderType
import org.librarysimplified.audiobook.manifest_fulfill.opa.OPAManifestURI
import org.librarysimplified.audiobook.manifest_fulfill.opa.OPAParameters
import org.librarysimplified.audiobook.manifest_fulfill.opa.OPAPassword
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentErrorType
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentEvent
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyType
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject
import java.net.URI

/**
 * The default audio book manifest strategy.
 */

internal class AudioBookManifestStrategy(
  private val request: AudioBookManifestRequest
) : AudioBookManifestStrategyType {

  private val logger =
    LoggerFactory.getLogger(AudioBookManifestStrategy::class.java)

  private val eventSubject =
    PublishSubject.create<String>()

  override val events: Observable<String> =
    this.eventSubject

  override fun execute(): TaskResult<String, AudioBookManifestData> {
    val taskRecorder = TaskRecorder.create<String>()

    try {
      val downloadResult =
        if (this.request.isNetworkAvailable()) {
          taskRecorder.beginNewStep("Downloading manifest…")
          this.downloadManifest()
        } else {
          taskRecorder.beginNewStep("Loading manifest…")
          this.loadFallbackManifest()
        }

      if (downloadResult is PlayerResult.Failure) {
        throw UnimplementedCodeException()
      }

      taskRecorder.beginNewStep("Parsing manifest…")
      val (contentType, downloadBytes) = (downloadResult as PlayerResult.Success).result
      val parseResult = this.parseManifest(this.request.targetURI, downloadBytes)
      if (parseResult is ParseResult.Failure) {
        throw UnimplementedCodeException()
      }

      taskRecorder.beginNewStep("Checking license…")
      val (_, parsedManifest) = parseResult as ParseResult.Success
      if (!this.checkManifest(parsedManifest)) {
        throw UnimplementedCodeException()
      }

      return this.finish(
        parsedManifest = parsedManifest,
        downloadBytes = downloadBytes,
        contentType = contentType,
        taskRecorder = taskRecorder
      )
    } catch (e: Exception) {
      throw UnimplementedCodeException(e)
    }
  }

  private data class DataLoadFailed(
    override val message: String,
    val exception: java.lang.Exception? = null,
    override val serverData: ManifestFulfillmentErrorType.ServerData? = null
  ) : ManifestFulfillmentErrorType

  private fun loadFallbackManifest(): PlayerResult<ManifestFulfilled, ManifestFulfillmentErrorType> {
    this.logger.debug("loadFallbackManifest")
    return try {
      val data = this.request.loadFallbackData()
      if (data == null) {
        PlayerResult.Failure(DataLoadFailed("No fallback manifest data is provided"))
      } else {
        PlayerResult.unit(data)
      }
    } catch (e: Exception) {
      this.logger.error("loadFallbackManifest: ", e)
      PlayerResult.Failure(DataLoadFailed(e.message ?: e.javaClass.name, e))
    }
  }

  private fun finish(
    parsedManifest: PlayerManifest,
    downloadBytes: ByteArray,
    contentType: MIMEType,
    taskRecorder: TaskRecorderType<String>
  ): TaskResult<String, AudioBookManifestData> {
    return taskRecorder.finishSuccess(
      AudioBookManifestData(
        manifest = parsedManifest,
        fulfilled = ManifestFulfilled(
          contentType = contentType,
          data = downloadBytes
        )
      )
    )
  }

  /**
   * @return `true` if the request content type implies an Overdrive audio book
   */

  private fun isOverdrive(): Boolean {
    return BookFormats.audioBookOverdriveMimeTypes()
      .map { it.fullType }
      .contains(this.request.contentType.fullType)
  }

  /**
   * Attempt to synchronously download a manifest file. If the download fails, return the
   * error details.
   */

  private fun downloadManifest(): PlayerResult<ManifestFulfilled, ManifestFulfillmentErrorType> {
    this.logger.debug("downloadManifest")

    val strategy: ManifestFulfillmentStrategyType =
      this.downloadStrategyForCredentials()
    val fulfillSubscription =
      strategy.events.subscribe(this::onManifestFulfillmentEvent)

    try {
      return strategy.execute()
    } finally {
      fulfillSubscription.unsubscribe()
    }
  }

  /**
   * Try to find an appropriate fulfillment strategy based on the audio book request.
   */

  private fun downloadStrategyForCredentials(): ManifestFulfillmentStrategyType {
    return if (this.isOverdrive()) {
      this.logger.debug("downloadStrategyForCredentials: trying an Overdrive strategy")

      val secretService =
        this.request.services.optionalService(
          AudioBookOverdriveSecretServiceType::class.java
        ) ?: throw UnsupportedOperationException("No Overdrive secret service is available")

      val strategies =
        this.request.strategyRegistry.findStrategy(
          OPAManifestFulfillmentStrategyProviderType::class.java
        ) ?: throw UnsupportedOperationException("No Overdrive fulfillment strategy is available")

      val parameters =
        when (val credentials = this.request.credentials) {
          is AudioBookCredentials.UsernamePassword ->
            OPAParameters(
              userName = credentials.userName,
              password = OPAPassword.Password(credentials.password),
              clientKey = secretService.clientKey,
              clientPass = secretService.clientPass,
              targetURI = OPAManifestURI.Indirect(this.request.targetURI),
              userAgent = this.request.userAgent
            )
          null -> {
            throw UnimplementedCodeException()
          }
        }

      strategies.create(parameters)
    } else {
      this.logger.debug("downloadStrategyForCredentials: trying a Basic strategy")

      val strategies =
        this.request.strategyRegistry.findStrategy(
          ManifestFulfillmentBasicType::class.java
        ) ?: throw UnsupportedOperationException("No Basic fulfillment strategy is available")

      val parameters =
        ManifestFulfillmentBasicParameters(
          uri = this.request.targetURI,
          credentials = this.request.credentials?.let { credentials ->
            when (credentials) {
              is AudioBookCredentials.UsernamePassword ->
                ManifestFulfillmentBasicCredentials(
                  userName = credentials.userName,
                  password = credentials.password
                )
            }
          },
          userAgent = this.request.userAgent
        )

      strategies.create(parameters)
    }
  }

  /**
   * Attempt to parse a manifest file.
   */

  private fun parseManifest(
    source: URI,
    data: ByteArray
  ): ParseResult<PlayerManifest> {
    this.logger.debug("parseManifest")
    return ManifestParsers.parse(
      uri = source,
      streams = data,
      extensions = this.request.extensions
    )
  }

  /**
   * Attempt to perform any required license checks on the manifest.
   */

  private fun checkManifest(
    manifest: PlayerManifest
  ): Boolean {
    this.logger.debug("checkManifest")

    val check =
      LicenseChecks.createLicenseCheck(
        LicenseCheckParameters(
          manifest = manifest,
          userAgent = this.request.userAgent,
          checks = this.request.licenseChecks
        )
      )

    val checkSubscription =
      check.events.subscribe { event ->
        this.onLicenseCheckEvent(event)
      }

    try {
      val checkResult = check.execute()
      return checkResult.checkSucceeded()
    } finally {
      checkSubscription.unsubscribe()
    }
  }

  private fun onLicenseCheckEvent(event: SingleLicenseCheckStatus) {
    this.logger.debug("onLicenseCheckEvent: {}: {}", event.source, event.message)
    this.eventSubject.onNext(event.message)
  }

  private fun onManifestFulfillmentEvent(event: ManifestFulfillmentEvent) {
    this.logger.debug("onManifestFulfillmentEvent: {}", event.message)
    this.eventSubject.onNext(event.message)
  }
}
