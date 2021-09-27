package org.nypl.simplified.accounts.source.filebased

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.json.AccountProvidersJSON
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

/**
 * A file-based account provider.
 */

class AccountProviderSourceFileBased(
  private val getFile: (Context) -> InputStream
) : AccountProviderSourceType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceFileBased::class.java)

  @Volatile
  private var cache: Map<URI, AccountProviderType>? = null

  override fun load(
    context: Context,
    includeTestingLibraries: Boolean
  ): SourceResult {
    val cached = this.cache
    if (cached != null) {
      this.logger.debug("returning cached providers")
      return SourceResult.SourceSucceeded(this.mapResult(cached))
    }

    return try {
      this.logger.debug("loading providers from file")
      this.getFile.invoke(context).use { stream ->
        val newResult = AccountProvidersJSON.deserializeCollectionFromStream(stream)
        this.logger.debug("loaded {} providers from file", newResult.size)
        this.cache = newResult
        SourceResult.SourceSucceeded(this.mapResult(newResult))
      }
    } catch (e: Exception) {
      this.logger.error("failed to load providers from file: ", e)
      SourceResult.SourceFailed(mapOf(), e)
    }
  }

  override fun query(
    context: Context,
    query: AccountSearchQuery
  ): SourceResult {
    return this.load(
      context = context,
      includeTestingLibraries = query.includeTestingLibraries
    )
  }

  override fun clear(context: Context) {
    this.cache = null
  }

  override fun canResolve(description: AccountProviderDescription): Boolean {
    return this.cache?.containsKey(description.id) ?: false
  }

  override fun resolve(
    onProgress: AccountProviderResolutionListenerType,
    description: AccountProviderDescription
  ): TaskResult<AccountProviderType> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Resolving account provider from file cache")

    val provider = this.cache?.get(description.id)
    return if (provider != null) {
      taskRecorder.finishSuccess(provider)
    } else {
      taskRecorder.finishFailure()
    }
  }

  private fun mapResult(cached: Map<URI, AccountProviderType>) =
    cached.mapValues { v -> v.value.toDescription() }
}
