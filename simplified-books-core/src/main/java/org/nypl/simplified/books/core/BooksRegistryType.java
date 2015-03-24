package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface BooksRegistryType
{
  OptionType<Book> bookGet(
    BookID id);

  void bookUpdate(
    Book b);
}
