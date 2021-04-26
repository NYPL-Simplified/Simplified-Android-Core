package org.nypl.simplified.accounts.source.nyplregistry

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountDistance
import org.nypl.simplified.accounts.api.AccountDistanceUnit.KILOMETERS
import org.nypl.simplified.accounts.api.AccountGeoLocation
import org.nypl.simplified.accounts.api.AccountLibraryLocation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParsersType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionSerializersType
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerConnectionFailure
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerReturnedError
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceResolutionStrings
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A server-based account provider.
 */

class AccountProviderSourceNYPLRegistry(
  private val http: LSHTTPClientType,
  private val authDocumentParsers: AuthenticationDocumentParsersType,
  private val parsers: AccountProviderDescriptionCollectionParsersType,
  private val serializers: AccountProviderDescriptionCollectionSerializersType,
  private val uriProduction: URI = URI("https://libraryregistry.librarysimplified.org/libraries"),
  private val uriQA: URI = URI("https://libraryregistry.librarysimplified.org/libraries/qa")
) : AccountProviderSourceType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceNYPLRegistry::class.java)

  /**
   * An intrinsic lock used to prevent multiple threads from overwriting the cached providers
   * on disk at the same time.
   */

  private val writeLock = Any()

  @Volatile
  private var stringResources: AccountProviderResolutionStringsType? = null

  private data class CacheFiles(
    val file: File,
    val fileTemp: File
  )

  /** The default time to retain the disk cache. */
  private val defaultCacheDuration = Duration.standardHours(4)

  override fun load(
    context: Context,
    includeTestingLibraries: Boolean
  ): SourceResult {
    if (this.stringResources == null) {
      this.stringResources =
        AccountProviderSourceResolutionStrings(
          context.resources
        )
    }

    val files = this.cacheFiles(context)
    val diskResults = this.fetchDiskResults(files)

    return try {
      /*
       * If we have a populated disk cache, and haven't exceeded the cache duration, then
       * just return the disk cache.
       */
      if (diskResults.isNotEmpty()) {
        val now = DateTime.now(DateTimeZone.UTC)
        val lastModifiedTime = DateTime(files.file.lastModified(), DateTimeZone.UTC)
        val age = Duration(lastModifiedTime, now)

        if (age.isShorterThan(this.defaultCacheDuration)) {
          this.logger.debug("disk cache is fresh; last-modified={}", lastModifiedTime)
          return SourceResult.SourceSucceeded(diskResults)
        } else {
          this.logger.debug("disk cache is expired; last-modified={}", lastModifiedTime)
        }
      }

      val serverResults =
        this.fetchServerResults(includeTestingLibraries)
      val mergedResults =
        this.mergeResults(diskResults, serverResults)

      this.cacheServerResults(files, mergedResults)
      SourceResult.SourceSucceeded(mergedResults)
    } catch (e: Exception) {
      this.logger.error("failed to fetch providers: ", e)
      SourceResult.SourceFailed(diskResults, e)
    }
  }

  override fun query(
    context: Context,
    query: AccountSearchQuery
  ): SourceResult {
    val result = this.load(
      context = context,
      includeTestingLibraries = query.includeTestingLibraries
    )

    /*
     * XXX: Fake testing data. This sets the location of every library to Fort Peck, and derives
     * a distance value from the library identifier.
     */

    return when (result) {
      is SourceResult.SourceFailed -> result
      is SourceResult.SourceSucceeded ->
        SourceResult.SourceSucceeded(
          result.results.mapValues { entry ->
            entry.value.copy(
              location = AccountLibraryLocation(
                location = AccountGeoLocation.Coordinates(
                  longitude = 48.009720957177684,
                  latitude = -106.42124352031094
                ),
                distance = AccountDistance(entry.value.id.hashCode() * 0.00001, KILOMETERS)
              )
            )
          }
        )
    }
  }

  override fun clear(context: Context) {
    synchronized(this.writeLock) {
      val files = this.cacheFiles(context)
      FileUtilities.fileDelete(files.file)
      FileUtilities.fileDelete(files.fileTemp)
    }
  }

  override fun canResolve(description: AccountProviderDescription): Boolean {
    /*
     * We assume that the NYPL registry can always resolve any account description.
     */

    return true
  }

  override fun resolve(
    onProgress: AccountProviderResolutionListenerType,
    description: AccountProviderDescription
  ): TaskResult<AccountProviderType> {
    return AccountProviderResolution(
      stringResources = this.stringResources!!,
      authDocumentParsers = this.authDocumentParsers,
      http = this.http,
      description = description
    ).resolve(onProgress)
  }

  private fun cacheFiles(context: Context): CacheFiles {
    return CacheFiles(
      file = File(context.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json"),
      fileTemp = File(context.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json.tmp")
    )
  }

  /**
   * Serialize the given set of provider descriptions. This serialized file will be used
   * every time this source is queried, and will be augmented with fresher descriptions
   * received from the server.
   */

  private fun cacheServerResults(
    cacheFiles: CacheFiles,
    mergedResults: Map<URI, AccountProviderDescription>
  ) {
    try {
      this.logger.debug("serializing cache: {}", cacheFiles.fileTemp)

      synchronized(this.writeLock) {
        cacheFiles.fileTemp.outputStream().use { stream ->
          val meta =
            AccountProviderDescriptionCollection.Metadata("")
          val collection =
            AccountProviderDescriptionCollection(
              providers = mergedResults.values.toList(),
              links = listOf(),
              metadata = meta
            )
          val serializer =
            this.serializers.createSerializer(cacheFiles.fileTemp.toURI(), stream, collection)
          serializer.serialize()
        }

        FileUtilities.fileRename(cacheFiles.fileTemp, cacheFiles.file)
      }
    } catch (e: Exception) {
      this.logger.debug("could not serialize cache: {}: ", cacheFiles.fileTemp, e)
    }
  }

  private fun mergeResults(
    diskResults: Map<URI, AccountProviderDescription>,
    serverResults: Map<URI, AccountProviderDescription>
  ): Map<URI, AccountProviderDescription> =
    diskResults.plus(serverResults)

  /**
   * Fetch the set of serialized provider descriptions.
   */

  private fun fetchDiskResults(cacheFiles: CacheFiles): Map<URI, AccountProviderDescription> {
    this.logger.debug("fetching disk cache: {}", cacheFiles.file)

    return try {
      cacheFiles.file.inputStream().use { stream ->
        val parser =
          this.parsers.createParser(cacheFiles.file.toURI(), stream)

        when (val result = parser.parse()) {
          is ParseResult.Failure -> {
            this.logParseFailure("cache", result)

            try {
              cacheFiles.file.delete()
            } catch (e: IOException) {
              this.logger.debug("could not delete cache file: {}: ", cacheFiles.file, e)
            }

            mapOf()
          }
          is ParseResult.Success -> {
            this.logger.debug(
              "loaded {} cached providers ({} warnings)",
              result.result.providers.size,
              result.warnings.size
            )

            this.logger.debug("{} cached providers", result.result.providers.size)
            result.result.providers.associateBy(AccountProviderDescription::id)
          }
        }
      }
    } catch (e: FileNotFoundException) {
      this.logger.debug("no cache file exists, skipping")
      emptyMap()
    } catch (e: Exception) {
      this.logger.debug("could not load cache file: ", e)
      emptyMap()
    }
  }

  /**
   * Fetch a set of provider descriptions from the server.
   */

  private fun fetchServerResults(
    includeTestingLibraries: Boolean
  ): Map<URI, AccountProviderDescription> {
    val results = if (includeTestingLibraries) {
      this.logger.debug("fetching QA providers from ${this.uriQA}")
      this.fetchAndParse(this.uriQA)
        .providers
        .associateBy { it.id }
    } else {
      this.logger.debug("fetching providers from ${this.uriProduction}")
      this.fetchAndParse(this.uriProduction)
        .providers
        .associateBy { it.id }
    }
    this.logger.debug("categorizing ${results.size} providers")
    return results
  }

  private fun fetchAndParse(target: URI): AccountProviderDescriptionCollection {
    return this.openStream(target).use { stream ->
      this.parseFromStream(target, stream)
    }
  }

  private fun parseFromStream(
    target: URI,
    stream: InputStream
  ): AccountProviderDescriptionCollection {
    return this.parsers.createParser(target, stream, warningsAsErrors = false).use { parser ->
      when (val parseResult = parser.parse()) {
        is ParseResult.Success ->
          parseResult.result
        is ParseResult.Failure -> {
          this.logParseFailure("server", parseResult)

          throw AccountProviderSourceNYPLRegistryException.ServerReturnedUnparseableData(
            uri = target,
            warnings = parseResult.warnings,
            errors = parseResult.errors
          )
        }
      }
    }
  }

  private fun logParseFailure(
    source: String,
    parseResult: ParseResult.Failure<AccountProviderDescriptionCollection>
  ) {
    this.logger.debug(
      "failed to parse providers from $source ({} errors, {} warnings)",
      parseResult.errors.size,
      parseResult.warnings.size
    )

    parseResult.errors.forEach { this.logger.error("parse error: {}: ", it.message) }
    parseResult.warnings.forEach { this.logger.warn("parse warning: {}: ", it.message) }
  }

  private fun openStream(target: URI): InputStream {
    val request =
      this.http.newRequest(target)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        status.bodyStream ?: ByteArrayInputStream(ByteArray(0))
      is LSHTTPResponseStatus.Responded.Error ->
        throw ServerReturnedError(
          uri = target,
          errorCode = status.properties.status,
          message = status.properties.message,
          problemReport = status.properties.problemReport
        )
      is LSHTTPResponseStatus.Failed ->
        throw ServerConnectionFailure(
          uri = target,
          cause = status.exception
        )
    }
  }
}
