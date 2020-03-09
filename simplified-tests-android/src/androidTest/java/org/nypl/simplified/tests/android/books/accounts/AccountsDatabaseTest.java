package org.nypl.simplified.tests.android.books.accounts;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.accounts.AccountsDatabaseContract;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class AccountsDatabaseTest extends AccountsDatabaseContract {

  private Context instrumentationContext;

  @Before
  public void setup() {
    super.setup();
    this.instrumentationContext = InstrumentationRegistry.getContext();
  }

  @Override
  protected Context context() {
    return this.instrumentationContext;
  }

}
