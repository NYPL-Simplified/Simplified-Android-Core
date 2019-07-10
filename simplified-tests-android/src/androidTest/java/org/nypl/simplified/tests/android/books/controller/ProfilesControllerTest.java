package org.nypl.simplified.tests.android.books.controller;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.controller.ProfilesControllerContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(AndroidJUnit4.class)
@MediumTest
public final class ProfilesControllerTest extends ProfilesControllerContract {

  private Context instrumentationContext;

  @Before
  public void setup() {
    this.instrumentationContext = InstrumentationRegistry.getContext();
  }

  @Override
  protected Context context() {
    return this.instrumentationContext;
  }

  @NotNull
  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(ProfilesControllerTest.class);
  }
}
