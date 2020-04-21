package org.nypl.simplified.tests.local.books.book_database;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.nypl.simplified.tests.books.book_database.BookDatabasePDFContract;

public final class BookDatabasePDFTest extends BookDatabasePDFContract {

  @NotNull
  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
