package org.nypl.simplified.tests.books.accounts;

import android.content.Context;

import org.mockito.Mockito;

public final class AccountsDatabaseTest extends AccountsDatabaseContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
