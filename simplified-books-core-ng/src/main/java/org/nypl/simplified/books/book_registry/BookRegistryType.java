package org.nypl.simplified.books.book_registry;

import org.nypl.simplified.books.book_database.BookID;

public interface BookRegistryType extends BookRegistryReadableType {

  void update(
      BookWithStatus status);

  void updateIfStatusIsMoreImportant(
      BookWithStatus status);

  void clear();

  void clearFor(
      BookID id);
}
