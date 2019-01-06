package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookStatusHeld;
import org.nypl.simplified.books.book_registry.BookStatusHeldReady;
import org.nypl.simplified.books.book_registry.BookStatusHoldable;
import org.nypl.simplified.books.book_registry.BookStatusLoanable;
import org.nypl.simplified.books.book_registry.BookStatusLoanedType;
import org.nypl.simplified.books.book_registry.BookStatusMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan;
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookStatusRevoked;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.feeds.FeedFacetPseudo;
import org.nypl.simplified.books.feeds.FeedFacetType;
import org.nypl.simplified.books.feeds.FeedSearchLocal;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

final class ProfileFeedTask implements Callable<FeedWithoutGroups> {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileFeedTask.class);

  private final ProfileFeedRequest request;
  private final BookRegistryReadableType book_registry;

  ProfileFeedTask(
      final BookRegistryReadableType book_registry,
      final ProfileFeedRequest request) {

    this.request =
        NullCheck.notNull(request, "Request");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
  }

  private static void facets(
      final ProfileFeedRequest request,
      final HashMap<String, List<FeedFacetType>> facet_groups,
      final ArrayList<FeedFacetType> facets)
  {
    final FeedFacetPseudo.FacetType[] values = FeedFacetPseudo.FacetType.values();
    for (final FeedFacetPseudo.FacetType v : values) {
      final boolean active = v.equals(request.facetActive());
      final FeedFacetPseudo f =
          new FeedFacetPseudo(request.facetTitleProvider().getTitle(v), active, v);
      facets.add(f);
    }
    facet_groups.put(request.facetGroup(), facets);
  }

  @Override
  public FeedWithoutGroups call() {

    LOG.debug("generating local feed");

    /*
     * Generate facets.
     */

    final HashMap<String, List<FeedFacetType>> facet_groups =
        new HashMap<>(32);
    final ArrayList<FeedFacetType> facets =
        new ArrayList<>(32);

    facets(this.request, facet_groups, facets);

    final FeedWithoutGroups feed =
        FeedWithoutGroups.newEmptyFeed(
            this.request.uri(),
            this.request.id(),
            Calendar.getInstance(),
            this.request.title(),
            Option.none(),
            Option.some(new FeedSearchLocal()),
            facet_groups,
            facets,
            Option.none(),
            Option.none(),
            Option.none(),
            Option.none());

    try {
      final ArrayList<BookWithStatus> books =
          collectAllBooks(this.book_registry);
      final BookStatusMatcherType<Boolean, UnreachableCodeException> filter =
          selectFeedFilter(this.request);

      filterBooks(filter, books);
      searchBooks(this.request.search(), books);
      sortBooks(this.request.facetActive(), books);

      for (final BookWithStatus book : books) {
        feed.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(book.book().entry()));
      }

      return feed;
    } finally {
      LOG.debug("generated a local feed with {} entries", feed.size());
    }
  }

  /**
   * @return {@code true} if any of the given search terms match the given book, or the list of
   * search terms is empty
   */

  private static boolean searchMatches(
      final List<String> terms_upper,
      final BookWithStatus book) {

    if (terms_upper.isEmpty()) {
      return true;
    }

    for (int index = 0; index < terms_upper.size(); ++index) {
      final String term_upper = terms_upper.get(index);
      final OPDSAcquisitionFeedEntry ee = book.book().entry();
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
   * Filter the given books by the given search terms.
   */

  private void searchBooks(
      final OptionType<String> search,
      final ArrayList<BookWithStatus> books) {

    final List<String> terms_upper = searchTermsSplitUpper(search);
    final Iterator<BookWithStatus> iter = books.iterator();
    while (iter.hasNext()) {
      final BookWithStatus book = iter.next();
      if (!searchMatches(terms_upper, book)) {
        iter.remove();
      }
    }
  }

  /**
   * Split the given search string into a list of uppercase search terms.
   */

  private List<String> searchTermsSplitUpper(final OptionType<String> search) {
    return search.accept(new OptionVisitorType<String, List<String>>() {
      @Override
      public List<String> none(final None<String> none) {
        return Collections.emptyList();
      }

      @Override
      public List<String> some(final Some<String> some) {
        final String[] terms = some.get().split("\\s+");
        final List<String> terms_upper = new ArrayList<String>(8);
        for (final String term : terms) {
          terms_upper.add(term.toUpperCase());
        }
        return terms_upper;
      }
    });
  }

  /**
   * Sort the list of books by the given facet.
   */

  private void sortBooks(
      final FeedFacetPseudo.FacetType facet,
      final ArrayList<BookWithStatus> books) {

    switch (facet) {
      case SORT_BY_AUTHOR:
        sortBooksByAuthor(books);
        break;
      case SORT_BY_TITLE:
        sortBooksByTitle(books);
        break;
    }
  }

  private void sortBooksByTitle(final ArrayList<BookWithStatus> books) {
    Collections.sort(books, (book0, book1) -> {
      final OPDSAcquisitionFeedEntry entry0 = book0.book().entry();
      final OPDSAcquisitionFeedEntry entry1 = book1.book().entry();
      return entry0.getTitle().compareTo(entry1.getTitle());
    });
  }

  private void sortBooksByAuthor(final ArrayList<BookWithStatus> books) {
    Collections.sort(books, (book0, book1) -> {
      final OPDSAcquisitionFeedEntry entry0 = book0.book().entry();
      final OPDSAcquisitionFeedEntry entry1 = book1.book().entry();
      final List<String> authors1 = entry0.getAuthors();
      final List<String> authors2 = entry1.getAuthors();
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
    });
  }

  /**
   * Filter the list of books with the given filter.
   */

  private void filterBooks(
      final BookStatusMatcherType<Boolean, UnreachableCodeException> filter,
      final ArrayList<BookWithStatus> books) {

    final Iterator<BookWithStatus> iter = books.iterator();
    while (iter.hasNext()) {
      final BookWithStatus book = iter.next();
      if (!book.status().matchBookStatus(filter)) {
        iter.remove();
      }
    }
  }

  private ArrayList<BookWithStatus> collectAllBooks(final BookRegistryReadableType book_registry) {
    return new ArrayList<>(book_registry.books().values());
  }

  private static BookStatusMatcherType<Boolean, UnreachableCodeException> selectFeedFilter(
      final ProfileFeedRequest request) {
    switch (request.feedSelection()) {
      case BOOKS_FEED_LOANED: {
        return new UsableForBooksFeed();
      }
      case BOOKS_FEED_HOLDS: {
        return new UsableForHoldsFeed();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * A status matcher that indicates if a book should be shown for "My Books" feeds.
   */

  private static final class UsableForBooksFeed
      implements BookStatusMatcherType<Boolean, UnreachableCodeException> {

    UsableForBooksFeed() {

    }

    @Override
    public Boolean onBookStatusHoldable(final BookStatusHoldable s) {
      return false;
    }

    @Override
    public Boolean onBookStatusHeld(final BookStatusHeld s) {
      return false;
    }

    @Override
    public Boolean onBookStatusHeldReady(final BookStatusHeldReady s) {
      return false;
    }

    @Override
    public Boolean onBookStatusLoanedType(final BookStatusLoanedType s) {
      return true;
    }

    @Override
    public Boolean onBookStatusRequestingLoan(final BookStatusRequestingLoan s) {
      return true;
    }

    @Override
    public Boolean onBookStatusRequestingRevoke(final BookStatusRequestingRevoke s) {
      return true;
    }

    @Override
    public Boolean onBookStatusLoanable(final BookStatusLoanable s) {
      return false;
    }

    @Override
    public Boolean onBookStatusRevokeFailed(final BookStatusRevokeFailed s) {
      return true;
    }

    @Override
    public Boolean onBookStatusRevoked(final BookStatusRevoked s) {
      return false;
    }
  }

  /**
   * A status matcher that indicates if a book should be shown for "Holds" feeds.
   */

  private static final class UsableForHoldsFeed
      implements BookStatusMatcherType<Boolean, UnreachableCodeException> {

    UsableForHoldsFeed() {

    }

    @Override
    public Boolean onBookStatusHoldable(final BookStatusHoldable s) {
      return true;
    }

    @Override
    public Boolean onBookStatusHeld(final BookStatusHeld s) {
      return true;
    }

    @Override
    public Boolean onBookStatusHeldReady(final BookStatusHeldReady s) {
      return true;
    }

    @Override
    public Boolean onBookStatusLoanedType(final BookStatusLoanedType s) {
      return false;
    }

    @Override
    public Boolean onBookStatusRequestingLoan(final BookStatusRequestingLoan s) {
      return false;
    }

    @Override
    public Boolean onBookStatusRequestingRevoke(final BookStatusRequestingRevoke s) {
      return false;
    }

    @Override
    public Boolean onBookStatusLoanable(final BookStatusLoanable s) {
      return false;
    }

    @Override
    public Boolean onBookStatusRevokeFailed(final BookStatusRevokeFailed s) {
      return false;
    }

    @Override
    public Boolean onBookStatusRevoked(final BookStatusRevoked s) {
      return false;
    }
  }
}
