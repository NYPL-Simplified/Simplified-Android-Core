package org.nypl.simplified.tests.local.books.controller;

import org.jetbrains.annotations.NotNull;
import org.nypl.simplified.tests.books.controller.BookBorrowTaskContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BookBorrowTaskTest extends BookBorrowTaskContract {

  @NotNull
  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(BookBorrowTaskTest.class);
  }
}
