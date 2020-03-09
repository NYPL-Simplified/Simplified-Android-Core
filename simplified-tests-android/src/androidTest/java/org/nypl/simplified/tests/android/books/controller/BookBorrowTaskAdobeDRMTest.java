package org.nypl.simplified.tests.android.books.controller;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.controller.BookBorrowTaskAdobeDRMContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(AndroidJUnit4.class)
@MediumTest
public final class BookBorrowTaskAdobeDRMTest extends BookBorrowTaskAdobeDRMContract {

  @NotNull
  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(BookBorrowTaskAdobeDRMTest.class);
  }
}
