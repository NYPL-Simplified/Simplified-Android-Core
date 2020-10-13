package org.nypl.simplified.tests.books.profiles;

import android.content.Context;

import org.mockito.Mockito;

public final class ProfilesDatabaseTest extends ProfilesDatabaseContract {
  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
