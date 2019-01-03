package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.observable.ObservableReadableType;

import java.util.NoSuchElementException;
import java.util.SortedMap;

/**
 * The type of readable book registries.
 */

public interface BookRegistryReadableType {

  /**
   * @return A read-only map of the known books
   */

  SortedMap<BookID, BookWithStatus> books();

  /**
   * @return An observable that publishes book status events
   */

  ObservableReadableType<BookStatusEvent> bookEvents();

  /**
   * @param id The book ID
   * @return The status for the given book, if any.
   */

  OptionType<BookStatusType> bookStatus(BookID id);

  /**
   * @param id The book ID
   * @return The registered book, if any
   */

  OptionType<BookWithStatus> book(BookID id);

  /**
   * @param id The book ID
   * @return The registered book
   * @throws NoSuchElementException If the given book does not exist
   */

  default BookWithStatus bookOrException(final BookID id)
      throws NoSuchElementException
  {
    return book(id).accept(new OptionVisitorType<BookWithStatus, BookWithStatus>() {
      @Override
      public BookWithStatus none(final None<BookWithStatus> none) {
        throw new NoSuchElementException("No such book: " + id.value());
      }

      @Override
      public BookWithStatus some(final Some<BookWithStatus> some) {
        return some.get();
      }
    });
  }
}
