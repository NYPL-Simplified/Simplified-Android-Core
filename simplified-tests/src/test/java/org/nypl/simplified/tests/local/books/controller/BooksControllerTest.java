package org.nypl.simplified.tests.local.books.controller;

import android.content.Context;

import org.mockito.Mockito;
import org.nypl.simplified.tests.books.controller.BooksControllerContract;

public final class BooksControllerTest extends BooksControllerContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
