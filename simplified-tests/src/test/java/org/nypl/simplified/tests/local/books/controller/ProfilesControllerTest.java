package org.nypl.simplified.tests.local.books.controller;

import android.content.Context;

import org.mockito.Mockito;
import org.nypl.simplified.tests.books.controller.ProfilesControllerContract;

public final class ProfilesControllerTest extends ProfilesControllerContract {

  @Override
  protected Context context() {
    return Mockito.mock(Context.class);
  }
}
