package org.nypl.simplified.accounts.source.filebased

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.json.AccountProvidersJSON
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

/**
 * A file-based account provider.
 */

class AccountProviderSourceFileBased(
  private val getFile: (Context) -> InputStream) : AccountProviderSourceType {

  private val logger = LoggerFactory.getLogger(AccountProviderSourceFileBased::class.java)

  /**
   * Secondary no-argument public constructor required for [java.util.ServiceLoader].
   */

  constructor() : this({ context -> context.assets.open("Accounts.json") })

  @Volatile
  private var cache: Map<URI, AccountProviderType>? = null

  override fun load(context: Context): SourceResult {
    val cached = this.cache
    if (cached != null) {
      this.logger.debug("returning cached providers")
      return SourceResult.SourceSucceeded(mapResult(cached))
    }

    return try {
      this.logger.debug("loading providers from file")
      this.getFile.invoke(context).use { stream ->
        val newResult = AccountProvidersJSON.deserializeCollectionFromStream(stream)
        this.logger.debug("loaded {} providers from file", newResult.size)
        this.cache = newResult
        SourceResult.SourceSucceeded(mapResult(newResult))
      }
    } catch (e: Exception) {
      this.logger.error("failed to load providers from file: ", e)
      SourceResult.SourceFailed(mapOf(), e)
    }
  }

  private fun mapResult(cached: Map<URI, AccountProviderType>) =
    cached.mapValues { v -> v.value.toDescription() }
}
