package org.nypl.simplified.accounts.source.nyplregistry

import android.content.Context
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParsersType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionSerializersType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionParsers
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionSerializers
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerConnectionFailure
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerReturnedError
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.parser.api.ParseResult
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ServiceLoader

/**
 * A server-based account provider.
 */

class AccountProviderSourceNYPLRegistry(
  private val http: HTTPType,
  private val authDocumentParsers: AuthenticationDocumentParsersType,
  private val parsers: AccountProviderDescriptionCollectionParsersType,
  private val serializers: AccountProviderDescriptionCollectionSerializersType) : AccountProviderSourceType {

  companion object {
    private fun findAuthenticationDocumentParsers(): AuthenticationDocumentParsersType {
      return ServiceLoader.load(AuthenticationDocumentParsersType::class.java)
        .firstOrNull()
        ?: throw IllegalStateException("No available implementation of type ${AuthenticationDocumentParsersType::class.java}")
    }
  }

  /**
   * Secondary no-arg constructor for use in [java.util.ServiceLoader].
   */

  constructor()
    : this(
    http = HTTP.newHTTP(),
    authDocumentParsers = findAuthenticationDocumentParsers(),
    parsers = AccountProviderDescriptionCollectionParsers(),
    serializers = AccountProviderDescriptionCollectionSerializers())

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceNYPLRegistry::class.java)
  private val uriProduction =
    URI("https://libraryregistry.librarysimplified.org/libraries")
  private val uriQA =
    URI("https://libraryregistry.librarysimplified.org/libraries/qa")

  /**
   * An intrinsic lock used to prevent multiple threads from overwriting the cached providers
   * on disk at the same time.
   */

  private val writeLock = Any()

  @Volatile
  private var firstCall = true

  @Volatile
  private var stringResources: AccountProviderResolutionStringsType? = null

  private data class CacheFiles(
    val file: File,
    val fileTemp: File)

  override fun load(context: Context): SourceResult {
    if (this.stringResources == null) {
      this.stringResources = AccountProviderSourceNYPLResolutionStrings(context.resources)
    }

    val files = this.cacheFiles(context)
    val diskResults = this.fetchDiskResults(files)

    /*
     * If we've not called the server before, and the disk cache is not empty, then just
     * return the disk cache. Otherwise, we need to call the server.
     */

    return try {
      if (this.firstCall) {
        if (diskResults.isNotEmpty()) {
          return SourceResult.SourceSucceeded(diskResults)
        }
      }

      val serverResults =
        this.fetchServerResults()
      val mergedResults =
        this.mergeResults(diskResults, serverResults)

      this.cacheServerResults(files, mergedResults)
      SourceResult.SourceSucceeded(mergedResults)
    } catch (e: Exception) {
      this.logger.error("failed to fetch providers: ", e)
      SourceResult.SourceFailed(diskResults, e)
    } finally {
      this.firstCall = false
    }
  }

  private fun cacheFiles(context: Context): CacheFiles {
    return CacheFiles(
      file = File(context.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json"),
      fileTemp = File(context.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json.tmp"))
  }

  /**
   * Serialize the given set of provider descriptions. This serialized file will be used
   * every time this source is queried, and will be augmented with fresher descriptions
   * received from the server.
   */

  private fun cacheServerResults(
    cacheFiles: CacheFiles,
    mergedResults: Map<URI, AccountProviderDescriptionType>) {

    try {
      this.logger.debug("serializing cache: {}", cacheFiles.fileTemp)

      synchronized(this.writeLock) {
        cacheFiles.fileTemp.outputStream().use { stream ->
          val meta =
            AccountProviderDescriptionCollection.Metadata(null, "")
          val collection =
            AccountProviderDescriptionCollection(
              providers = mergedResults.values.map { p -> p.metadata },
              links = listOf(),
              metadata = meta)
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
    diskResults: Map<URI, AccountProviderDescriptionType>,
    serverResults: Map<URI, AccountProviderDescriptionType>
  ): Map<URI, AccountProviderDescriptionType> =
    diskResults.plus(serverResults)

  /**
   * Fetch the set of serialized provider descriptions.
   */

  private fun fetchDiskResults(cacheFiles: CacheFiles): Map<URI, AccountProviderDescriptionType> {
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
            this.logger.debug("loaded {} cached providers ({} warnings)",
              result.result.providers.size,
              result.warnings.size)
            result.result.providers
              .map { provider ->
                Pair(
                  provider.id,
                  AccountProviderSourceNYPLRegistryDescription(
                    stringResources = this.stringResources!!,
                    authDocumentParsers = this.authDocumentParsers,
                    http = this.http,
                    metadata = provider
                  )
                )
              }
              .toMap()
          }
        }
      }
    } catch (e: Exception) {
      this.logger.debug("could not load cache file: ", e)
      mapOf()
    }
  }

  /**
   * Fetch a set of provider descriptions from the server.
   */

  private fun fetchServerResults(): Map<URI, AccountProviderDescriptionType> {
    this.logger.debug("fetching production providers from ${this.uriProduction}")
    val sourceProductionProviders =
      this.fetchAndParse(this.uriProduction)
        .providers
        .map { p -> Pair(p.id, p) }
        .toMap()

    this.logger.debug("fetching QA providers from ${this.uriQA}")
    val sourceQaProviders =
      this.fetchAndParse(this.uriQA)
        .providers
        .map { p -> Pair(p.id, p) }
        .toMap()

    this.logger.debug("categorizing ${sourceQaProviders.size} providers")
    val results = mutableMapOf<URI, AccountProviderDescriptionType>()
    for (id in sourceQaProviders.keys) {
      val sourceMetadata =
        sourceQaProviders[id]!!
      val resultMetadata =
        sourceMetadata.copy(isProduction = sourceProductionProviders.containsKey(id))

      Preconditions.checkState(
        !results.containsKey(id),
        "ID $id must not already be present in the results")

      results[id] = AccountProviderSourceNYPLRegistryDescription(
        stringResources = this.stringResources!!,
        authDocumentParsers = this.authDocumentParsers,
        http = this.http,
        metadata = resultMetadata)
    }
    return results.toMap()
  }

  private fun fetchAndParse(target: URI): AccountProviderDescriptionCollection {
    return this.openStream(target).use { stream ->
      this.parseFromStream(target, stream)
    }
  }

  private fun parseFromStream(target: URI, stream: InputStream): AccountProviderDescriptionCollection {
    return this.parsers.createParser(target, stream, warningsAsErrors = false).use { parser ->
      when (val parseResult = parser.parse()) {
        is ParseResult.Success ->
          parseResult.result
        is ParseResult.Failure -> {
          this.logParseFailure("server", parseResult)

          throw AccountProviderSourceNYPLRegistryException.ServerReturnedUnparseableData(
            uri = target,
            warnings = parseResult.warnings,
            errors = parseResult.errors)
        }
      }
    }
  }

  private fun logParseFailure(
    source: String,
    parseResult: ParseResult.Failure<AccountProviderDescriptionCollection>) {
    this.logger.debug("failed to parse providers from $source ({} errors, {} warnings)",
      parseResult.errors.size,
      parseResult.warnings.size)

    parseResult.errors.forEach { this.logger.error("parse error: {}: ", it.message) }
    parseResult.warnings.forEach { this.logger.warn("parse warning: {}: ", it.message) }
  }

  private fun openStream(target: URI): InputStream {
    return when (val connectResult = this.http.get(Option.none(), target, 0)) {
      is HTTPResultOK ->
        connectResult.value
      is HTTPResultError ->
        throw ServerReturnedError(
          uri = target,
          errorCode = connectResult.status,
          message = connectResult.message,
          problemReport = this.someOrNull(connectResult.problemReport))
      is HTTPResultException ->
        throw ServerConnectionFailure(
          uri = target,
          cause = connectResult.error)

      // XXX: Somebody should seal the HTTPResultType class...
      else -> throw UnreachableCodeException()
    }
  }

  private fun <T> someOrNull(o: OptionType<T>): T? =
    if (o is Some<T>) {
      o.get()
    } else {
      null
    }
}
