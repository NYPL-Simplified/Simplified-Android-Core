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
import org.nypl.simplified.links.Link
import org.nypl.simplified.opds2.OPDS2Catalog
import org.nypl.simplified.opds2.OPDS2Contributor
import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.opds2.OPDS2Group
import org.nypl.simplified.opds2.OPDS2Metadata
import org.nypl.simplified.opds2.OPDS2Name
import org.nypl.simplified.opds2.OPDS2Navigation
import org.nypl.simplified.opds2.OPDS2Publication
import org.nypl.simplified.opds2.OPDS2Title

/**
 * Functions to convert Irradia feeds to Simplified feeds.
 */

internal object OPDS2IrradiaFeeds {

  /**
   * Convert an Irradia OPDS 2.0 feed to a Simplified OPDS 2.0 feed. This is very nearly
   * an identity conversion.
   */

  fun convert(
    result: OPDS20Feed
  ): OPDS2Feed {
    val catalogs =
      result.extensions.find { e -> e is OPDS20CatalogList } as OPDS20CatalogList?

    return OPDS2Feed(
      uri = result.uri,
      metadata = convertMetadata(result.metadata),
      navigation = result.navigation?.let(this::convertNavigation),
      publications = result.publications.map(this::convertPublication),
      groups = result.groups.map(this::convertGroup),
      links = result.links.map(this::convertLink),
      catalogs = catalogs?.let(this::convertCatalogs) ?: listOf()
    )
  }

  private fun convertCatalogs(
    catalogList: OPDS20CatalogList
  ): List<OPDS2Catalog> {
    return catalogList.catalogs.map(this::convertCatalog)
  }

  private fun convertCatalog(
    catalog: OPDS20Catalog
  ): OPDS2Catalog {
    return OPDS2Catalog(
      metadata = convertMetadata(catalog.metadata),
      links = catalog.links.map(this::convertLink)
    )
  }

  private fun convertGroup(
    group: OPDS20Group
  ): OPDS2Group {
    return OPDS2Group(
      metadata = convertMetadata(group.metadata),
      navigation = group.navigation?.let(this::convertNavigation),
      publications = group.publications.map(this::convertPublication),
      links = group.links.map(this::convertLink)
    )
  }

  private fun convertPublication(
    publication: OPDS20Publication
  ): OPDS2Publication {
    return OPDS2Publication(
      metadata = convertMetadata(publication.metadata),
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

  private fun convertMetadata(
    metadata: OPDS20Metadata
  ): OPDS2Metadata {
    return OPDS2Metadata(
      identifier = metadata.identifier,
      title = convertTitle(metadata.title),
      subtitle = metadata.subtitle?.let { convertTitle(it) },
      modified = metadata.modified,
      published = metadata.published,
      languages = metadata.languages,
      sortAs = metadata.sortAs,
      author = metadata.author.map(this::convertAuthor)
    )
  }

  private fun convertAuthor(
    contributor: OPDS20Contributor
  ): OPDS2Contributor {
    return OPDS2Contributor(
      name = convertName(contributor.name),
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
