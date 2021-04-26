package org.nypl.simplified.accounts.json

import org.nypl.simplified.accounts.api.AccountGeoLocation
import org.nypl.simplified.accounts.api.AccountLibraryLocation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParserType
import org.nypl.simplified.opds2.OPDS2Catalog
import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.opds2.OPDS2Metadata
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.parser.api.ParserType
import org.slf4j.LoggerFactory

/**
 * A parser of provider description collections.
 */

class AccountProviderDescriptionCollectionParser internal constructor(
  private val parser: ParserType<OPDS2Feed>
) : AccountProviderDescriptionCollectionParserType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderDescriptionCollectionParser::class.java)

  private lateinit var feed: OPDS2Feed
  private val errors = mutableListOf<ParseError>()
  private val warnings = mutableListOf<ParseWarning>()

  private fun logError(
    message: String
  ) {
    this.errors.add(
      ParseError(
        source = this.feed.uri,
        message = message,
        line = 0,
        column = 0,
        exception = null
      )
    )
  }

  private fun logWarning(
    message: String
  ) {
    this.warnings.add(
      ParseWarning(
        source = this.feed.uri,
        message = message,
        line = 0,
        column = 0,
        exception = null
      )
    )
  }

  override fun parse(): ParseResult<AccountProviderDescriptionCollection> {
    return this.parser.parse().flatMap(this::processFeed)
  }

  private fun processFeed(feed: OPDS2Feed): ParseResult<AccountProviderDescriptionCollection> {
    this.feed = feed

    val metadata =
      this.processMetadata(feed.metadata)
    val accountDescriptions =
      feed.catalogs.mapNotNull(this::processCatalog)

    if (this.errors.isEmpty()) {
      return ParseResult.Success(
        warnings = this.warnings.toList(),
        result = AccountProviderDescriptionCollection(
          providers = accountDescriptions,
          links = feed.links,
          metadata = metadata
        )
      )
    }

    return ParseResult.Failure(
      warnings = this.warnings.toList(),
      errors = this.errors.toList()
    )
  }

  private fun processCatalog(
    catalog: OPDS2Catalog
  ): AccountProviderDescription? {
    val errorsThen = this.errors.size

    val id = catalog.metadata.identifier
    if (id == null) {
      this.logError("An identifier is required for catalog ${catalog.metadata.title.title}")
    }

    val updated = catalog.metadata.modified
    if (updated == null) {
      this.logError("An 'updated' time is required for catalog ${catalog.metadata.title.title}")
    }

    val location = catalog.metadata.location?.let { location ->
      this.parseLocation(location, catalog.metadata.distance)
    }

    val errorsNow = this.errors.size
    if (errorsNow != errorsThen) {
      return null
    }

    return AccountProviderDescription(
      id = id!!,
      title = catalog.metadata.title.title,
      updated = updated!!,
      links = catalog.links,
      images = listOf(),
      isAutomatic = catalog.metadata.isAutomatic,
      isProduction = catalog.metadata.isProduction,
      location = location
    )
  }

  private fun parseLocation(
    location: String,
    distance: String?
  ): AccountLibraryLocation? {
    try {
      val segments = location.split(',')
      if (segments.size != 2) {
        this.logWarning("Expected a lat/long coordinate pair. Received '$location'")
        return null
      }

      val latitude = segments[0].trim().toDouble()
      val longitude = segments[1].trim().toDouble()
      return AccountLibraryLocation(
        location = AccountGeoLocation.Coordinates(longitude, latitude),
        distance = null
      )
    } catch (e: Exception) {
      this.logger.warn("parse warning: ", e)
      this.logWarning("Could not parse library location")
      return null
    }
  }

  private fun processMetadata(
    metadata: OPDS2Metadata
  ): AccountProviderDescriptionCollection.Metadata {
    return AccountProviderDescriptionCollection.Metadata(
      title = metadata.title.title
    )
  }

  override fun close() {
    this.parser.close()
  }
}
