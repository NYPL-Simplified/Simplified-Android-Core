package org.nypl.simplified.books.audio

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.manifest_fulfill.api.ManifestFulfillmentStrategies
import org.librarysimplified.audiobook.manifest_fulfill.api.ManifestFulfillmentStrategyRegistryType
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsersType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.services.api.ServiceDirectoryType
import java.io.File
import java.net.URI
import java.util.ServiceLoader

/**
 * A request to fulfill, parse, and license check an audio book manifest.
 */

data class AudioBookManifestRequest(

  /**
   * The target URI of the manifest. This must be a stable URI that can be accessed repeatedly
   * in an idempotent manner. In practice, this will be the "fulfill" URI provided by the
   * Circulation Manager.
   */

  val targetURI: URI,

  /**
   * The content type of the target manifest.
   */

  val contentType: MIMEType,

  /**
   * The user agent string used for any HTTP requests.
   */

  val userAgent: PlayerUserAgent,

  /**
   * The credentials used for any requests.
   */

  val credentials: AudioBookCredentials?,

  /**
   * A service directory used to locate any required application services.
   */

  val services: ServiceDirectoryType,

  /**
   * A function that returns `true` if networking is currently available.
   */

  val isNetworkAvailable: () -> Boolean = { true },

  /**
   * A function that will be evaluated if networking is not available. The function
   * should return the raw bytes of a manifest. If the function returns `null`, the manifest
   * strategy must fail.
   */

  val loadFallbackData: () -> ManifestFulfilled? = { null },

  /**
   * The set of license checks to perform. The default value searches for license checks
   * registered with [ServiceLoader] on the classpath.
   */

  val licenseChecks: List<SingleLicenseCheckProviderType> =
    ServiceLoader.load(SingleLicenseCheckProviderType::class.java)
      .toList(),

  /**
   * The set of parser extensions to use. The default value searches for parser extensions
   * registered with [ServiceLoader] on the classpath.
   */

  val extensions: List<ManifestParserExtensionType> =
    ServiceLoader.load(ManifestParserExtensionType::class.java)
      .toList(),

  /**
   * A registry of manifest fulfillment strategies. The default value uses the AudioBook API
   * registry.
   */

  val strategyRegistry: ManifestFulfillmentStrategyRegistryType =
    ManifestFulfillmentStrategies,

  /**
   * The manifest parser API.
   */

  val manifestParsers: ManifestParsersType =
    ManifestParsers,

  /**
   * The directory in which to store cache files.
   */

  val cacheDirectory: File
)
