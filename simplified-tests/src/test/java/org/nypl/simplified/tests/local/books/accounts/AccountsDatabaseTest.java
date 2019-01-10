package org.nypl.simplified.tests.local.books.accounts;

import android.content.Context;

import org.mockito.Mockito;
import org.nypl.simplified.tests.books.accounts.AccountsDatabaseContract;

public final class AccountsDatabaseTest extends AccountsDatabaseContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
