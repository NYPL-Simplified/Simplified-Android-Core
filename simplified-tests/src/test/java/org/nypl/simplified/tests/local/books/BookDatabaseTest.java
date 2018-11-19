package org.nypl.simplified.tests.local.books;

import android.content.Context;
import org.mockito.Mockito;
import org.nypl.simplified.tests.books.BookDatabaseContract;

public final class BookDatabaseTest extends BookDatabaseContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }

}
