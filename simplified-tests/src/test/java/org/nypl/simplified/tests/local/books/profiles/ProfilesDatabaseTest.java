package org.nypl.simplified.tests.local.books.profiles;

import android.content.Context;

import org.mockito.Mockito;
import org.nypl.simplified.tests.books.profiles.ProfilesDatabaseContract;

public final class ProfilesDatabaseTest extends ProfilesDatabaseContract {
  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
