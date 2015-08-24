package org.nypl.simplified.books.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class BooksControllerFeedTask implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerFeedTask.class));
  }

  private final BookDatabaseType                 books_database;
  private final FacetType                        facet_active;
  private final String                           facet_group;
  private final FeedFacetPseudoTitleProviderType facet_titles;
  private final String                           id;
  private final BookFeedListenerType             listener;
  private final String                           title;
  private final Calendar                         updated;
  private final URI                              uri;
  private final OptionType<String>               search;
  private final BooksFeedSelection               selection;

  BooksControllerFeedTask(
    final BookDatabaseType in_books_database,
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final FeedFacetPseudo.FacetType in_facet_active,
    final String in_facet_group,
    final FeedFacetPseudoTitleProviderType in_facet_titles,
    final OptionType<String> in_search,
    final BooksFeedSelection in_selection,
    final BookFeedListenerType in_listener)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.facet_active = NullCheck.notNull(in_facet_active);
    this.facet_group = NullCheck.notNull(in_facet_group);
    this.facet_titles = NullCheck.notNull(in_facet_titles);
    this.search = NullCheck.notNull(in_search);
    this.selection = NullCheck.notNull(in_selection);
    this.listener = NullCheck.notNull(in_listener);
  }

  /**
   * Load all the entries that are applicable for the selected type of feed.
   *
   * @param f         The feed being built
   * @param dirs      The list of database entries
   * @param selection The type of feed
   * @param entries   The resulting entries
   */

  private static void entriesLoad(
    final FeedWithoutGroups f,
    final List<BookDatabaseEntryType> dirs,
    final BooksFeedSelection selection,
    final AbstractList<FeedEntryType> entries)
  {
    for (int index = 0; index < dirs.size(); ++index) {
      final BookDatabaseEntryReadableType dir =
        NullCheck.notNull(dirs.get(index));
      final BookID book_id = dir.getID();

      try {
        final OPDSAcquisitionFeedEntry data = dir.getData();
        final OPDSAvailabilityType availability = data.getAvailability();

        final Boolean use = availability.matchAvailability(
          BooksControllerFeedTask.matcherForSelection(selection));

        if (use.booleanValue()) {
          entries.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(data));
        }
      } catch (final Throwable x) {
        BooksControllerFeedTask.LOG.error(
          "unable to load book {} metadata: ", book_id, x);
        f.add(FeedEntryCorrupt.fromIDAndError(book_id, x));
      }
    }
  }

  /**
   * Select an entry matcher based on the type of feed.
   */

  private static OPDSAvailabilityMatcherType<Boolean,
    UnreachableCodeException> matcherForSelection(
    final BooksFeedSelection selection)
  {
    switch (selection) {
      case BOOKS_FEED_LOANED:
        return new UsableForBooksFeed();
      case BOOKS_FEED_HOLDS:
        return new UsableForHoldsFeed();
    }

    throw new UnreachableCodeException();
  }

  /**
   * Sort the feed entries according to the given facet type.
   *
   * @param entries    The entries to be sorted
   * @param facet_type The facet type
   */

  private static void entriesSortForFacet(
    final List<FeedEntryType> entries,
    final FacetType facet_type)
  {
    switch (facet_type) {
      case SORT_BY_AUTHOR: {
        Collections.sort(
          entries, new Comparator<FeedEntryType>()
          {
            @Override public int compare(
              final @Nullable FeedEntryType o1,
              final @Nullable FeedEntryType o2)
            {
              final FeedEntryType o1_n = NullCheck.notNull(o1);
              final FeedEntryType o2_n = NullCheck.notNull(o2);

              if ((o1_n instanceof FeedEntryOPDS)
                  && (o2_n instanceof FeedEntryOPDS)) {
                final FeedEntryOPDS fo1 = (FeedEntryOPDS) o1_n;
                final FeedEntryOPDS fo2 = (FeedEntryOPDS) o2_n;
                final List<String> authors1 = fo1.getFeedEntry().getAuthors();
                final List<String> authors2 = fo2.getFeedEntry().getAuthors();
                final boolean e0 = authors1.isEmpty();
                final boolean e1 = authors2.isEmpty();
                if (e0 && e1) {
                  return 0;
                }
                if (e0) {
                  return 1;
                }
                if (e1) {
                  return -1;
                }

                final String author1 = NullCheck.notNull(authors1.get(0));
                final String author2 = NullCheck.notNull(authors2.get(0));
                return author1.compareTo(author2);
              }

              return 0;
            }
          });
        break;
      }
      case SORT_BY_TITLE: {
        Collections.sort(
          entries, new Comparator<FeedEntryType>()
          {
            @Override public int compare(
              final @Nullable FeedEntryType o1,
              final @Nullable FeedEntryType o2)
            {
              final FeedEntryType o1_n = NullCheck.notNull(o1);
              final FeedEntryType o2_n = NullCheck.notNull(o2);

              if ((o1_n instanceof FeedEntryOPDS)
                  && (o2_n instanceof FeedEntryOPDS)) {
                final FeedEntryOPDS fo1 = (FeedEntryOPDS) o1_n;
                final FeedEntryOPDS fo2 = (FeedEntryOPDS) o2_n;
                final String title1 = fo1.getFeedEntry().getTitle();
                final String title2 = fo2.getFeedEntry().getTitle();
                return title1.compareTo(title2);
              }

              return 0;
            }
          });
        break;
      }
    }
  }

  /**
   * Search the given feed entry for any of the given terms.
   *
   * @param terms_upper The terms
   * @param e           The feed entry
   *
   * @return {@code true} if the feed entry matches
   */

  private static boolean entriesSearchFeedEntryOPDSMatches(
    final List<String> terms_upper,
    final FeedEntryOPDS e)
  {
    for (int index = 0; index < terms_upper.size(); ++index) {
      final String term_upper = terms_upper.get(index);
      final OPDSAcquisitionFeedEntry ee = e.getFeedEntry();
      final String e_title = ee.getTitle().toUpperCase();
      if (e_title.contains(term_upper)) {
        return true;
      }

      final List<String> authors = ee.getAuthors();
      for (final String a : authors) {
        if (a.toUpperCase().contains(term_upper)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Search all entries for the given search terms. The search is case
   * insensitive.
   *
   * @param entries The entries
   * @param terms   The search terms, separated by spaces
   */

  private static void entriesSearch(
    final List<FeedEntryType> entries,
    final String terms)
  {
    final List<String> terms_upper =
      BooksControllerFeedTask.entriesSearchTermsSplitUpper(terms);

    final FeedEntryMatcherType<Boolean, UnreachableCodeException> matcher =
      new FeedEntryMatcherType<Boolean, UnreachableCodeException>()
      {
        @Override public Boolean onFeedEntryOPDS(
          final FeedEntryOPDS e)
        {
          return Boolean.valueOf(
            BooksControllerFeedTask.entriesSearchFeedEntryOPDSMatches(
              terms_upper, e));
        }

        @Override public Boolean onFeedEntryCorrupt(
          final FeedEntryCorrupt e)
        {
          return Boolean.FALSE;
        }
      };

    final Iterator<FeedEntryType> iter = entries.iterator();
    while (iter.hasNext()) {
      final FeedEntryType e = iter.next();
      final Boolean ok = e.matchFeedEntry(matcher);
      if (ok.booleanValue() == false) {
        iter.remove();
      }
    }
  }

  /**
   * Split a string into a set of uppercase search terms.
   *
   * @param term The terms
   */

  private static List<String> entriesSearchTermsSplitUpper(
    final String term)
  {
    final String[] terms = term.split("\\s+");
    final List<String> terms_upper = new ArrayList<String>(8);
    for (int index = 0; index < terms.length; ++index) {
      terms_upper.add(terms[index].toUpperCase());
    }
    return terms_upper;
  }

  private FeedWithoutGroups feed()
  {
    final OptionType<URI> no_next = Option.none();

    final OptionType<FeedSearchType> some_search =
      Option.some((FeedSearchType) new FeedSearchLocal());

    final Map<String, List<FeedFacetType>> facet_groups =
      new HashMap<String, List<FeedFacetType>>(32);
    final List<FeedFacetType> facets = new ArrayList<FeedFacetType>(32);

    final FacetType[] values = FacetType.values();
    for (final FeedFacetPseudo.FacetType v : values) {
      final boolean active = v.equals(this.facet_active);
      final FeedFacetPseudo f =
        new FeedFacetPseudo(this.facet_titles.getTitle(v), active, v);
      facets.add(f);
    }
    facet_groups.put(this.facet_group, facets);

    final OptionType<URI> no_terms = Option.none();
    final OptionType<URI> no_privacy = Option.none();
    final FeedWithoutGroups f = FeedWithoutGroups.newEmptyFeed(
      this.uri,
      this.id,
      this.updated,
      this.title,
      no_next,
      some_search,
      facet_groups,
      facets,
      no_terms,
      no_privacy);

    final List<BookDatabaseEntryType> dirs =
      this.books_database.getBookDatabaseEntries();

    final AbstractList<FeedEntryType> entries =
      new ArrayList<FeedEntryType>(dirs.size());

    BooksControllerFeedTask.entriesLoad(f, dirs, this.selection, entries);
    this.search.map(
      new FunctionType<String, Unit>()
      {
        @Override public Unit call(final String terms)
        {
          BooksControllerFeedTask.entriesSearch(entries, terms);
          return Unit.unit();
        }
      });
    BooksControllerFeedTask.entriesSortForFacet(entries, this.facet_active);

    for (int index = 0; index < entries.size(); ++index) {
      f.add(entries.get(index));
    }

    return f;
  }

  @Override public void run()
  {
    try {
      this.listener.onBookFeedSuccess(this.feed());
    } catch (final Throwable x) {
      this.listener.onBookFeedFailure(x);
    }
  }

  /**
   * An availability matcher that indicates if a book should be shown for "My
   * Books" feeds.
   */

  private static final class UsableForBooksFeed
    implements OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException>
  {
    UsableForBooksFeed()
    {

    }

    @Override public Boolean onHeldReady(final OPDSAvailabilityHeldReady a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onHeld(final OPDSAvailabilityHeld a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onHoldable(final OPDSAvailabilityHoldable a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onLoaned(final OPDSAvailabilityLoaned a)
    {
      return Boolean.TRUE;
    }

    @Override public Boolean onLoanable(final OPDSAvailabilityLoanable a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onOpenAccess(final OPDSAvailabilityOpenAccess a)
    {
      return Boolean.TRUE;
    }
  }

  /**
   * An availability matcher that indicates if a book should be shown for
   * "Holds" feeds.
   */

  private static final class UsableForHoldsFeed
    implements OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException>
  {
    UsableForHoldsFeed()
    {

    }

    @Override public Boolean onHeldReady(final OPDSAvailabilityHeldReady a)
    {
      return Boolean.TRUE;
    }

    @Override public Boolean onHeld(final OPDSAvailabilityHeld a)
    {
      return Boolean.TRUE;
    }

    @Override public Boolean onHoldable(final OPDSAvailabilityHoldable a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onLoaned(final OPDSAvailabilityLoaned a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onLoanable(final OPDSAvailabilityLoanable a)
    {
      return Boolean.FALSE;
    }

    @Override public Boolean onOpenAccess(final OPDSAvailabilityOpenAccess a)
    {
      return Boolean.FALSE;
    }
  }
}
