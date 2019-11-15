package org.nypl.simplified.tests.android.books.controller;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.controller.BookRevokeTaskContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(AndroidJUnit4.class)
@MediumTest
public final class BookRevokeTaskTest extends BookRevokeTaskContract {

  @NotNull
  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(BookRevokeTaskTest.class);
  }
}
