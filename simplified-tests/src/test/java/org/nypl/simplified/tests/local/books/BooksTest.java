package org.nypl.simplified.tests.local.books;

import android.content.Context;

import org.mockito.Mockito;
import org.nypl.simplified.tests.books.BooksContract;

public final class BooksTest extends BooksContract {
  
  @Override
  protected Context getContext() {
    return Mockito.mock(Context.class);
  }
}
