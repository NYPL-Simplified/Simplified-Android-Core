package org.nypl.simplified.books.feeds;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.books.book_database.BookID;

import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A (mutable) feed without groups.
 */

public final class FeedWithoutGroups extends AbstractList<FeedEntryType> implements FeedType
{
  private final Map<BookID, FeedEntryType>       entries;
  private final List<BookID>                     entries_order;
  private final Map<String, List<FeedFacetType>> facets_by_group;
  private final List<FeedFacetType>              facets_order;
  private final String                           id;
  private final OptionType<URI>                  next;
  private final OptionType<FeedSearchType>       search;
  private final String                           title;
  private final Calendar                         updated;
  private final URI                              uri;
  private final OptionType<URI>                  terms_of_service;
  private final OptionType<URI>                  privacy_policy;
  private final OptionType<URI>                  about;
  private final OptionType<URI>                  licenses;

  private FeedWithoutGroups(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<FeedSearchType> in_search,
    final List<BookID> in_entries_order,
    final Map<BookID, FeedEntryType> in_entries,
    final Map<String, List<FeedFacetType>> in_facets_by_group,
    final List<FeedFacetType> in_facets_order,
    final OptionType<URI> in_terms_of_service,
    final OptionType<URI> in_about,
    final OptionType<URI> in_privacy_policy,
    final OptionType<URI> in_licenses)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.next = NullCheck.notNull(in_next);
    this.search = NullCheck.notNull(in_search);
    this.entries_order = NullCheck.notNull(in_entries_order);
    this.entries = NullCheck.notNull(in_entries);
    this.facets_by_group = NullCheck.notNull(in_facets_by_group);
    this.facets_order = NullCheck.notNull(in_facets_order);
    this.terms_of_service = NullCheck.notNull(in_terms_of_service);
    this.about = NullCheck.notNull(in_about);
    this.privacy_policy = NullCheck.notNull(in_privacy_policy);
    this.licenses = NullCheck.notNull(in_licenses);
  }

  /**
   * Construct an empty feed.
   *
   * @param in_uri              The feed URI
   * @param in_id               The feed ID
   * @param in_updated          The last updated time
   * @param in_title            The title
   * @param in_next             A link to the next part of the feed, if any
   * @param in_search           A link to the feed searcher, if any
   * @param in_facets_by_group  The facets arranged by group
   * @param in_facets_order     The facets arranged in order
   * @param in_terms_of_service An optional link to the terms of service for the
   *                            feed
   * @param in_about            An optional link to the about for the feed
   * @param in_privacy_policy   An optional link to the privacy policy for the
   *                            feed
   *
   * @param in_licenses          An optional link to the licenses for the feed
   * @return An empty feed
   */

  public static FeedWithoutGroups newEmptyFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<FeedSearchType> in_search,
    final Map<String, List<FeedFacetType>> in_facets_by_group,
    final List<FeedFacetType> in_facets_order,
    final OptionType<URI> in_terms_of_service,
    final OptionType<URI> in_about,
    final OptionType<URI> in_privacy_policy,
    final OptionType<URI> in_licenses)
  {
    final List<BookID> in_entries_order = new ArrayList<BookID>(32);
    final Map<BookID, FeedEntryType> in_entries =
      new ConcurrentHashMap<BookID, FeedEntryType>(32);

    return new FeedWithoutGroups(
      in_uri,
      in_id,
      in_updated,
      in_title,
      in_next,
      in_search,
      in_entries_order,
      in_entries,
      in_facets_by_group,
      in_facets_order,
      in_terms_of_service,
      in_about,
      in_privacy_policy,
      in_licenses);
  }

  @Override public void add(
    final int index,
    final @Nullable FeedEntryType element)
  {
    final FeedEntryType nn_element = NullCheck.notNull(element);
    final BookID book_id = nn_element.getBookID();
    if (!this.entries.containsKey(book_id)) {
      this.entries_order.add(index, book_id);
      this.entries.put(book_id, nn_element);
    }
  }

  /**
   * @return A link to the terms of service, if any
   */

  public OptionType<URI> getFeedTermsOfService()
  {
    return this.terms_of_service;
  }

  @Override public FeedEntryType get(
    final int index)
  {
    final BookID book_id = NullCheck.notNull(this.entries_order.get(index));
    return NullCheck.notNull(this.entries.get(book_id));
  }

  /**
   * @return The feed facets by group
   */

  public Map<String, List<FeedFacetType>> getFeedFacetsByGroup()
  {
    return this.facets_by_group;
  }

  /**
   * @return The feed facets in order
   */

  public List<FeedFacetType> getFeedFacetsOrder()
  {
    return this.facets_order;
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  /**
   * @return A link to the privacy policy, if any
   */

  public OptionType<URI> getFeedPrivacyPolicy()
  {
    return this.privacy_policy;
  }

  /**
   * @return A link to the about, if any
   */

  public OptionType<URI> getFeedAbout()
  {
    return this.about;
  }

  /**
   * @return A link to the licenses, if any
   */

  public OptionType<URI> getFeedLicenses()
  {
    return this.licenses;
  }

  /**
   * @return A link to the next part of the feed, if any
   */

  public OptionType<URI> getFeedNext()
  {
    return this.next;
  }

  @Override public OptionType<FeedSearchType> getFeedSearch()
  {
    return this.search;
  }

  @Override public String getFeedTitle()
  {
    return this.title;
  }

  @Override public Calendar getFeedUpdated()
  {
    return this.updated;
  }

  @Override public URI getFeedURI()
  {
    return this.uri;
  }

  @Override public <A, E extends Exception> A matchFeed(
    final FeedMatcherType<A, E> m)
    throws E
  {
    return m.onFeedWithoutGroups(this);
  }

  @Override public FeedEntryType remove(
    final int index)
  {
    final BookID book_id = NullCheck.notNull(this.entries_order.get(index));
    final FeedEntryType r = NullCheck.notNull(this.entries.remove(book_id));
    this.entries_order.remove(index);
    return r;
  }

  @Override public FeedEntryType set(
    final int index,
    final @Nullable FeedEntryType element)
  {
    final FeedEntryType nn_element = NullCheck.notNull(element);
    final BookID book_id = NullCheck.notNull(this.entries_order.get(index));
    final FeedEntryType old = NullCheck.notNull(this.entries.get(book_id));
    this.entries.put(book_id, nn_element);
    return old;
  }

  @Override public int size()
  {
    return this.entries_order.size();
  }

  /**
   * Update the entry with the same ID as {@code e}. If no such entry exists, do
   * nothing.
   *
   * @param e The entry
   */

  public void updateEntry(final FeedEntryType e)
  {
    NullCheck.notNull(e, "Entry");

    final BookID book_id = e.getBookID();
    if (this.entries.containsKey(book_id)) {
      this.entries.put(book_id, e);
    }
  }

  /**
   * @param in_id The book ID
   *
   * @return {@code true} iff the feed contains an entry with {@code in_id}
   */

  public boolean containsID(final BookID in_id)
  {
    NullCheck.notNull(in_id, "ID");
    return this.entries.containsKey(in_id);
  }
}
