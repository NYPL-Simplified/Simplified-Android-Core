package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nypl.simplified.opds.core.OPDSSearchLink;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class FeedWithoutGroups extends AbstractList<FeedEntryType> implements
  FeedType
{
  public static FeedWithoutGroups newEmptyFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<OPDSSearchLink> in_search)
  {
    final List<BookID> in_entries_order = new ArrayList<BookID>();
    final Map<BookID, FeedEntryType> in_entries =
      new HashMap<BookID, FeedEntryType>();
    return new FeedWithoutGroups(
      in_uri,
      in_id,
      in_updated,
      in_title,
      in_next,
      in_search,
      in_entries_order,
      in_entries);
  }

  private final Map<BookID, FeedEntryType> entries;
  private final List<BookID>               entries_order;
  private final String                     id;
  private final OptionType<URI>            next;
  private final OptionType<OPDSSearchLink> search;
  private final String                     title;
  private final Calendar                   updated;
  private final URI                        uri;

  private FeedWithoutGroups(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<OPDSSearchLink> in_search,
    final List<BookID> in_entries_order,
    final Map<BookID, FeedEntryType> in_entries)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.next = NullCheck.notNull(in_next);
    this.search = NullCheck.notNull(in_search);
    this.entries_order = NullCheck.notNull(in_entries_order);
    this.entries = NullCheck.notNull(in_entries);
  }

  @Override public void add(
    final int index,
    final @Nullable FeedEntryType element)
  {
    final FeedEntryType nn_element = NullCheck.notNull(element);
    final BookID book_id = nn_element.getBookID();
    this.entries_order.add(index, book_id);
    this.entries.put(book_id, nn_element);
  }

  @Override public FeedEntryType get(
    final int index)
  {
    final BookID book_id = NullCheck.notNull(this.entries_order.get(index));
    return NullCheck.notNull(this.entries.get(book_id));
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  public OptionType<URI> getFeedNext()
  {
    return this.next;
  }

  @Override public OptionType<OPDSSearchLink> getFeedSearchURI()
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
}
