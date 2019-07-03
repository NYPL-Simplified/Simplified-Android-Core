package org.nypl.simplified.accounts.source.nyplregistry

import android.content.Context
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParsersType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionResult
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionParsers
import org.nypl.simplified.accounts.source.api.AccountProviderSourceType
import org.nypl.simplified.accounts.source.api.AccountProviderSourceType.SourceResult
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerConnectionFailure
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryException.ServerReturnedError
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

/**
 * A server-based account provider.
 */

class AccountProviderSourceNYPLRegistry(
  val http: HTTPType,
  val parsers: AccountProviderDescriptionCollectionParsersType) : AccountProviderSourceType {

  /**
   * Secondary no-arg constructor for use in [java.util.ServiceLoader].
   */

  constructor() : this(HTTP.newHTTP(), AccountProviderDescriptionCollectionParsers())

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceNYPLRegistry::class.java)
  private val uriProduction =
    URI("https://libraryregistry.librarysimplified.org/libraries")
  private val uriQA =
    URI("https://libraryregistry.librarysimplified.org/libraries/qa")

  override fun load(context: Context): SourceResult {
    return try {
      this.logger.debug("fetching production providers from $uriProduction")
      val sourceProductionProviders =
        this.fetchAndParse(this.uriProduction)
          .providers
          .map { p -> Pair(p.id, p) }
          .toMap()

      this.logger.debug("fetching QA providers from $uriQA")
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

        results[id] = Description(resultMetadata)
      }

      SourceResult.SourceSucceeded(results.toMap())
    } catch (e: Exception) {
      SourceResult.SourceFailed(e)
    }
  }

  private class Description(
    override val metadata: AccountProviderDescriptionMetadata): AccountProviderDescriptionType {

    override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
      val taskRecorder =
        TaskRecorder.create<AccountProviderResolutionErrorDetails>()

      taskRecorder.beginNewStep("Resolving...")
      taskRecorder.currentStepFailed("Failed!", null, null)
      onProgress.invoke(this.metadata.id, "Resolving...")
      return AccountProviderResolutionResult(null, taskRecorder.finish())
    }
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
        is ParseResult.Failure ->
          throw AccountProviderSourceNYPLRegistryException.ServerReturnedUnparseableData(
            uri = target,
            warnings = parseResult.warnings,
            errors = parseResult.errors)
      }
    }
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
          problemReport = someOrNull(connectResult.problemReport))
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
