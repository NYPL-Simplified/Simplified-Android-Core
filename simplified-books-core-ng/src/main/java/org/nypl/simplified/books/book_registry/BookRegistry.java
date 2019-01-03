package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.logging.LogUtilities;
import org.nypl.simplified.observable.Observable;
import org.nypl.simplified.observable.ObservableReadableType;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_CHANGED;
import static org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_REMOVED;

public final class BookRegistry implements BookRegistryType {

  private static final Logger LOG = LogUtilities.getLog(BookRegistry.class);

  private final SortedMap<BookID, BookWithStatus> books_read_only;
  private final ConcurrentSkipListMap<BookID, BookWithStatus> books;
  private final ObservableType<BookStatusEvent> observable;

  private BookRegistry(
      final ConcurrentSkipListMap<BookID, BookWithStatus> books) {

    this.books = NullCheck.notNull(books, "Books");
    this.books_read_only = Collections.unmodifiableSortedMap(books);
    this.observable = Observable.create();
  }

  public static BookRegistryType create() {
    return new BookRegistry(new ConcurrentSkipListMap<>());
  }

  @Override
  public SortedMap<BookID, BookWithStatus> books() {
    return this.books_read_only;
  }

  @Override
  public ObservableReadableType<BookStatusEvent> bookEvents() {
    return this.observable;
  }

  @Override
  public OptionType<BookStatusType> bookStatus(final BookID id) {
    return book(id).map(BookWithStatus::status);
  }

  @Override
  public OptionType<BookWithStatus> book(final BookID id) {
    return Option.of(this.books.get(NullCheck.notNull(id, "id")));
  }

  @Override
  public void update(
      final BookWithStatus update) {

    NullCheck.notNull(update, "Update");
    this.books.put(update.book().id(), update);
    this.observable.send(BookStatusEvent.create(update.book().id(), BOOK_CHANGED));
  }

  @Override
  public void updateIfStatusIsMoreImportant(
      final BookWithStatus update) {

    NullCheck.notNull(update, "Update");

    final BookWithStatus current = this.books.get(update.book().id());
    if (current != null) {
      final BookStatusPriorityOrdering current_p = current.status().getPriority();
      final BookStatusPriorityOrdering update_p = update.status().getPriority();

      if (current_p.getPriority() <= update_p.getPriority()) {
        LOG.debug("current {} <= {}, updating", current, update);
        this.update(update);
        return;
      }

      LOG.debug("current {} > {}, not updating", current, update);
    } else {
      this.update(update);
    }
  }

  @Override
  public void clear() {
    final HashSet<BookID> ids = new HashSet<>(books.keySet());
    this.books.clear();
    for (final BookID id : ids) {
      this.observable.send(BookStatusEvent.create(id, BOOK_REMOVED));
    }
  }

  @Override
  public void clearFor(
      final BookID id) {
    this.books.remove(NullCheck.notNull(id, "ID"));
    this.observable.send(BookStatusEvent.create(id, BOOK_REMOVED));
  }
}
