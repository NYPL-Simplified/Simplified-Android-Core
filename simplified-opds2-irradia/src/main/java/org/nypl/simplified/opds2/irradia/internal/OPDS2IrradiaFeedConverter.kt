package org.nypl.simplified.opds2.irradia.internal

import one.irradia.opds2_0.api.OPDS20Contributor
import one.irradia.opds2_0.api.OPDS20Feed
import one.irradia.opds2_0.api.OPDS20Group
import one.irradia.opds2_0.api.OPDS20Link
import one.irradia.opds2_0.api.OPDS20Metadata
import one.irradia.opds2_0.api.OPDS20Name
import one.irradia.opds2_0.api.OPDS20Navigation
import one.irradia.opds2_0.api.OPDS20Publication
import one.irradia.opds2_0.api.OPDS20Title
import one.irradia.opds2_0.library_simplified.api.OPDS20Catalog
import one.irradia.opds2_0.library_simplified.api.OPDS20CatalogList
import one.irradia.opds2_0.library_simplified.api.OPDS20CatalogMetadata
import org.nypl.simplified.links.Link
import org.nypl.simplified.opds2.OPDS2Catalog
import org.nypl.simplified.opds2.OPDS2CatalogMetadata
import org.nypl.simplified.opds2.OPDS2Contributor
import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.opds2.OPDS2Group
import org.nypl.simplified.opds2.OPDS2Metadata
import org.nypl.simplified.opds2.OPDS2Name
import org.nypl.simplified.opds2.OPDS2Navigation
import org.nypl.simplified.opds2.OPDS2Publication
import org.nypl.simplified.opds2.OPDS2Title
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning

class OPDS2IrradiaFeedConverter(
  private val feed: OPDS20Feed
) {

  private val warnings =
    mutableListOf<ParseWarning>()
  private val errors =
    mutableListOf<ParseError>()

  fun convert(): ParseResult<OPDS2Feed> {
    val catalogs =
      this.feed.extensions.find { e -> e is OPDS20CatalogList } as OPDS20CatalogList?
        ?: OPDS20CatalogList(listOf())

    val catalogResults =
      catalogs.catalogs.mapNotNull(this::convertCatalog)
    val metadata =
      this.convertMetadata(this.feed.metadata)
    val navigation =
      this.feed.navigation?.let(this::convertNavigation)
    val publications =
      this.feed.publications.map(this::convertPublication)
    val groups =
      this.feed.groups.map(this::convertGroup)
    val links =
      this.feed.links.map(this::convertLink)

    if (this.errors.isNotEmpty()) {
      return ParseResult.Failure(
        warnings = this.warnings.toList(),
        errors = this.errors.toList()
      )
    }

    return ParseResult.Success(
      warnings = this.warnings.toList(),
      result = OPDS2Feed(
        uri = this.feed.uri,
        metadata = metadata,
        navigation = navigation,
        publications = publications,
        groups = groups,
        links = links,
        catalogs = catalogResults
      )
    )
  }

  private fun convertGroup(
    group: OPDS20Group
  ): OPDS2Group {
    return OPDS2Group(
      metadata = this.convertMetadata(group.metadata),
      navigation = group.navigation?.let(this::convertNavigation),
      publications = group.publications.map(this::convertPublication),
      links = group.links.map(this::convertLink)
    )
  }

  private fun convertPublication(
    publication: OPDS20Publication
  ): OPDS2Publication {
    return OPDS2Publication(
      metadata = this.convertMetadata(publication.metadata),
      links = publication.links.map(this::convertLink),
      readingOrder = publication.readingOrder.map(this::convertLink),
      resources = publication.resources.map(this::convertLink),
      tableOfContents = publication.tableOfContents.map(this::convertLink),
      images = publication.images.map(this::convertLink)
    )
  }

  private fun convertNavigation(
    navigation: OPDS20Navigation
  ): OPDS2Navigation {
    return OPDS2Navigation(
      links = navigation.links.map(this::convertLink)
    )
  }

  private fun convertCatalog(
    catalog: OPDS20Catalog
  ): OPDS2Catalog? {
    val catalogMetadata =
      catalog.metadata.extensions.find { e -> e is OPDS20CatalogMetadata } as OPDS20CatalogMetadata?

    if (catalogMetadata == null) {
      this.errors.add(
        ParseError(
          source = this.feed.uri,
          message = "Catalog '${catalog.metadata.title}' is missing NYPL metadata",
          line = 0,
          column = 0,
          exception = null
        )
      )
      return null
    }

    return OPDS2Catalog(
      metadata = this.convertCatalogMetadata(
        metadata = catalog.metadata,
        catalogMetadata = catalogMetadata
      ),
      links = catalog.links.map(this::convertLink)
    )
  }

  private fun convertMetadata(
    metadata: OPDS20Metadata
  ): OPDS2Metadata {
    return OPDS2Metadata(
      identifier = metadata.identifier,
      title = this.convertTitle(metadata.title),
      subtitle = metadata.subtitle?.let(this::convertTitle),
      modified = metadata.modified,
      published = metadata.published,
      languages = metadata.languages,
      sortAs = metadata.sortAs,
      author = metadata.author.map(this::convertAuthor)
    )
  }

  private fun convertCatalogMetadata(
    metadata: OPDS20Metadata,
    catalogMetadata: OPDS20CatalogMetadata,
  ): OPDS2CatalogMetadata {
    return OPDS2CatalogMetadata(
      identifier = catalogMetadata.id ?: metadata.identifier,
      title = this.convertTitle(metadata.title),
      subtitle = metadata.subtitle?.let(this::convertTitle),
      modified = catalogMetadata.updated ?: metadata.modified,
      published = metadata.published,
      languages = metadata.languages,
      sortAs = metadata.sortAs,
      author = metadata.author.map(this::convertAuthor),
      isAutomatic = catalogMetadata.isAutomatic,
      isProduction = catalogMetadata.isProduction,
      location = catalogMetadata.location,
      distance = catalogMetadata.distance,
      libraryType = catalogMetadata.libraryType
    )
  }

  private fun convertAuthor(
    contributor: OPDS20Contributor
  ): OPDS2Contributor {
    return OPDS2Contributor(
      name = this.convertName(contributor.name),
      identifier = contributor.identifier,
      sortAs = contributor.sortAs,
      links = contributor.links.map(this::convertLink)
    )
  }

  private fun convertLink(
    link: OPDS20Link
  ): Link {
    return when (link) {
      is OPDS20Link.OPDS20LinkBasic ->
        Link.LinkBasic(
          href = link.href,
          type = link.type,
          relation = link.relations.sorted().joinToString(" "),
          title = link.title,
          height = link.height,
          width = link.width,
          duration = link.duration,
          bitrate = link.bitrate
        )
      is OPDS20Link.OPDS20LinkTemplated ->
        Link.LinkTemplated(
          href = link.href,
          type = link.type,
          relation = link.relations.sorted().joinToString(" "),
          title = link.title,
          height = link.height,
          width = link.width,
          duration = link.duration,
          bitrate = link.bitrate
        )
    }
  }

  private fun convertName(
    name: OPDS20Name
  ): OPDS2Name {
    return OPDS2Name(
      name = name.name,
      byLanguage = name.byLanguage
    )
  }

  private fun convertTitle(
    title: OPDS20Title
  ): OPDS2Title {
    return OPDS2Title(title.title)
  }
}
