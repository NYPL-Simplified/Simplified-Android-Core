package org.nypl.simplified.tests.books.controller;

import android.content.Context;

import org.mockito.Mockito;

public final class BooksControllerTest extends BooksControllerContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
