package org.nypl.simplified.feeds.api

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.NullCheck
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedSearch.FeedSearchOpen1_1
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

/**
 * The type of mutable feeds.
 *
 * This provides an abstraction over parsed OPDS feeds, and locally generated
 * feeds from book databases. The user can match on the values using [ ][.matchFeed] to determine the real type of the feed.
 */

sealed class Feed {

  /**
   * @return The unique identifier of the feed
   */

  abstract val feedID: String

  /**
   * @return The search URI for the feed
   */

  abstract val feedSearch: FeedSearch?

  /**
   * @return The title of the feed
   */

  abstract val feedTitle: String

  /**
   * @return The last time the feed was updated
   */

  abstract val feedUpdated: DateTime

  /**
   * @return The URI of the feed
   */

  abstract val feedURI: URI

  /**
   * @return The number of entries in the feed
   */

  abstract val size: Int

  data class FeedWithoutGroups internal constructor(
    override val feedID: String,
    override val feedSearch: FeedSearch?,
    override val feedTitle: String,
    override val feedUpdated: DateTime,
    override val feedURI: URI,

    /**
     * @return A link to the terms of service, if any
     */

    val feedTermsOfService: URI?,

    /**
     * @return A link to the privacy policy, if any
     */

    val feedPrivacyPolicy: URI?,

    /**
     * @return A link to the about, if any
     */

    val feedAbout: URI?,

    /**
     * @return A link to the licenses, if any
     */

    val feedLicenses: URI?,

    /**
     * @return A link to the next part of the feed, if any
     */

    val feedNext: URI?,

    private val facetsByGroupData: MutableMap<String, MutableList<FeedFacet>>,
    private val facetsOrderData: MutableList<FeedFacet>
  ) : Feed() {

    private val entriesData: MutableMap<BookID, FeedEntry> = mutableMapOf()
    private val entriesOrderData: MutableList<BookID> = mutableListOf()

    val facetsByGroup: Map<String, List<FeedFacet>> =
      Collections.unmodifiableMap(this.facetsByGroupData)
    val facetsOrder: List<FeedFacet> =
      Collections.unmodifiableList(this.facetsOrderData)
    val entriesByID: Map<BookID, FeedEntry> =
      Collections.unmodifiableMap(this.entriesData)

    val entriesInOrder: MutableList<FeedEntry> = object : AbstractMutableList<FeedEntry>() {
      override val size: Int
        get() = entriesOrderData.size

      override fun add(index: Int, element: FeedEntry) {
        if (!entriesData.containsKey(element.bookID)) {
          entriesOrderData.add(index, element.bookID)
          entriesData.put(element.bookID, element)
        }
      }

      override fun get(index: Int): FeedEntry {
        return entriesData[entriesOrderData.get(index)]!!
      }

      override fun removeAt(index: Int): FeedEntry {
        val bookID = entriesOrderData.get(index)
        val removed = entriesData.remove(bookID)
        entriesOrderData.removeAt(index)
        return removed!!
      }

      override fun set(index: Int, element: FeedEntry): FeedEntry {
        val bookID = entriesOrderData.get(index)
        val old = entriesData[bookID]
        entriesData[bookID] = element
        return old!!
      }
    }

    override val size: Int
      get() = this.entriesOrderData.size

    fun containsBook(bookID: BookID): Boolean {
      return this.entriesByID.containsKey(bookID)
    }
  }

  data class FeedWithGroups internal constructor(
    override val feedID: String,
    override val feedSearch: FeedSearch?,
    override val feedTitle: String,
    override val feedUpdated: DateTime,
    override val feedURI: URI,

    /**
     * @return A link to the terms of service, if any
     */

    val feedTermsOfService: URI?,

    /**
     * @return A link to the privacy policy, if any
     */

    val feedPrivacyPolicy: URI?,

    /**
     * @return A link to the about, if any
     */

    val feedAbout: URI?,

    /**
     * @return A link to the licenses, if any
     */

    val feedLicenses: URI?,

    /**
     * @return A link to the next part of the feed, if any
     */

    val feedNext: URI?,

    private val facetsByGroupData: MutableMap<String, MutableList<FeedFacet>>,
    private val facetsOrderData: MutableList<FeedFacet>,
    private val feedGroupsData: MutableMap<String, FeedGroup>,
    private val feedGroupsOrderData: MutableList<String>
  ) : Feed() {

    val facetsByGroup: Map<String, List<FeedFacet>> =
      Collections.unmodifiableMap(this.facetsByGroupData)
    val facetsOrder: List<FeedFacet> =
      Collections.unmodifiableList(this.facetsOrderData)
    val feedGroupsByID: Map<String, FeedGroup> =
      Collections.unmodifiableMap(this.feedGroupsData)
    val feedGroupsInOrder: MutableList<FeedGroup> =
      object : AbstractMutableList<FeedGroup>() {
        override val size: Int
          get() = feedGroupsOrderData.size

        override fun add(index: Int, element: FeedGroup) {
          val name = element.groupTitle
          feedGroupsOrderData.add(index, name)
          feedGroupsData.put(name, element)
        }

        override fun get(index: Int): FeedGroup {
          return feedGroupsData[feedGroupsOrderData.get(index)]!!
        }

        override fun removeAt(index: Int): FeedGroup {
          val name = feedGroupsOrderData.get(index)
          val removed = feedGroupsData.remove(name)
          feedGroupsOrderData.removeAt(index)
          return removed!!
        }

        override fun set(index: Int, element: FeedGroup): FeedGroup {
          val name = feedGroupsOrderData.get(index)
          val old = feedGroupsData[name]
          feedGroupsData[name] = element
          return old!!
        }
      }

    override val size: Int
      get() = this.feedGroupsOrderData.size
  }

  companion object {

    /**
     * Construct an empty feed without groups.
     */

    fun empty(
      feedID: String,
      feedSearch: FeedSearch?,
      feedTitle: String,
      feedURI: URI,
      feedFacets: List<FeedFacet>,
      feedFacetGroups: Map<String, List<FeedFacet>>
    ): FeedWithoutGroups {

      val mutableGroups =
        feedFacetGroups.mapValues { entry ->
          entry.value.toMutableList()
        }.toMutableMap()

      return FeedWithoutGroups(
        feedID = feedID,
        feedSearch = feedSearch,
        feedTitle = feedTitle,
        feedUpdated = DateTime.now(),
        feedAbout = null,
        feedURI = feedURI,
        feedTermsOfService = null,
        feedPrivacyPolicy = null,
        feedLicenses = null,
        feedNext = null,
        facetsOrderData = feedFacets.toMutableList(),
        facetsByGroupData = mutableGroups
      )
    }

    /**
     * Construct a feed from the given acquisition feed.
     *
     * @param accountId The account that owns the entries in the feed
     * @param feed The feed
     * @param search The search document
     * @return A new feed
     */

    fun fromAcquisitionFeed(
      accountId: AccountID,
      feed: OPDSAcquisitionFeed,
      search: OPDSOpenSearch1_1?
    ): Feed {

      return if (feed.feedGroups.isEmpty()) {
        withoutGroups(accountId, feed, search)
      } else {
        withGroups(accountId, feed, search)
      }
    }

    private fun <T> mapNull(option: OptionType<T>): T? {
      if (option is Some<T>) {
        return option.get()
      } else {
        return null
      }
    }

    private fun withoutGroups(
      accountId: AccountID,
      feed: OPDSAcquisitionFeed,
      search: OPDSOpenSearch1_1?
    ): FeedWithoutGroups {

      val facetsByGroup =
        constructFacetGroups(feed)
      val facetsOrder =
        constructFacetsOrdered(feed)
      val actualSearch =
        if (search != null) {
          FeedSearchOpen1_1(search)
        } else {
          null
        }

      val result =
        FeedWithoutGroups(
          feedID = feed.feedID,
          feedNext = mapNull(feed.feedNext),
          feedLicenses = mapNull(feed.feedLicenses),
          feedSearch = actualSearch,
          feedTitle = feed.feedTitle,
          feedUpdated = feed.feedUpdated,
          feedURI = feed.feedURI,
          feedTermsOfService = mapNull(feed.feedTermsOfService),
          feedPrivacyPolicy = mapNull(feed.feedPrivacyPolicy),
          feedAbout = mapNull(feed.feedAbout),
          facetsByGroupData = facetsByGroup,
          facetsOrderData = facetsOrder
        )

      val entries = feed.feedEntries
      for (index in entries.indices) {
        val feedEntry = entries[index]
        if (!feedEntry.acquisitions.isEmpty()) {
          var best = feedEntry.acquisitions[0]
          for (current in feedEntry.acquisitions) {
            if (priority(current) > priority(best)) {
              best = current
            }
          }
          if (best != null) {
            result.entriesInOrder.add(FeedEntry.FeedEntryOPDS(accountId, feedEntry))
          }
        }
      }

      return result
    }

    private fun withGroups(
      accountId: AccountID,
      feed: OPDSAcquisitionFeed,
      search: OPDSOpenSearch1_1?
    ): FeedWithGroups {

      val facetsByGroup =
        constructFacetGroups(feed)
      val facetsOrder =
        constructFacetsOrdered(feed)

      val actualSearch =
        if (search != null) {
          FeedSearchOpen1_1(search)
        } else {
          null
        }

      return FeedWithGroups(
        feedID = feed.feedID,
        feedSearch = actualSearch,
        feedTitle = feed.feedTitle,
        feedUpdated = feed.feedUpdated,
        feedURI = feed.feedURI,
        feedTermsOfService = mapNull(feed.feedTermsOfService),
        feedPrivacyPolicy = mapNull(feed.feedPrivacyPolicy),
        feedAbout = mapNull(feed.feedAbout),
        feedLicenses = mapNull(feed.feedLicenses),
        feedNext = mapNull(feed.feedNext),
        facetsByGroupData = facetsByGroup,
        facetsOrderData = facetsOrder,
        feedGroupsData = FeedGroup.fromOPDSGroups(accountId, feed.getFeedGroups()),
        feedGroupsOrderData = feed.feedGroupsOrder
      )
    }

    private fun constructFacetsOrdered(f: OPDSAcquisitionFeed): MutableList<FeedFacet> {
      val facetsOrder = ArrayList<FeedFacet>(4)
      for (ff in f.feedFacetsOrder) {
        facetsOrder.add(FeedFacet.FeedFacetOPDS(NullCheck.notNull(ff)))
      }
      return facetsOrder
    }

    private fun constructFacetGroups(feed: OPDSAcquisitionFeed): MutableMap<String, MutableList<FeedFacet>> {
      val facetsByGroup = HashMap<String, MutableList<FeedFacet>>(4)
      val fMap = feed.feedFacetsByGroup
      for (k in fMap.keys) {
        val fs = fMap[k]!!
        val rs = ArrayList<FeedFacet>(4)
        for (ff in fs) {
          rs.add(FeedFacet.FeedFacetOPDS(NullCheck.notNull(ff)))
        }
        facetsByGroup[k] = rs
      }
      return facetsByGroup
    }

    private fun priority(acquisition: OPDSAcquisition): Int {
      return when (acquisition.relation) {
        ACQUISITION_BORROW -> 6
        ACQUISITION_OPEN_ACCESS -> 5
        ACQUISITION_GENERIC -> 4
        ACQUISITION_SAMPLE -> 3
        ACQUISITION_BUY -> 2
        ACQUISITION_SUBSCRIBE -> 1
      }
    }
  }
}
