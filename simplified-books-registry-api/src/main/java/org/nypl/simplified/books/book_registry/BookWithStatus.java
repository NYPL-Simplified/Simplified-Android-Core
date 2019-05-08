package org.nypl.simplified.books.book_registry;

import com.google.auto.value.AutoValue;

import org.nypl.simplified.books.api.Book;

@AutoValue
public abstract class BookWithStatus {

  BookWithStatus() {

  }

  public abstract Book book();

  public abstract BookStatusType status();

  public static BookWithStatus create(
      final Book book,
      final BookStatusType status)
  {
    return new AutoValue_BookWithStatus(book, status);
  }
}
