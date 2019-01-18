package org.nypl.simplified.tests.android.books;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.BookDatabaseContract;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class BookDatabaseTest extends BookDatabaseContract {

  private Context instrumentationContext;

  @Before
  public void setup() {
    this.instrumentationContext = InstrumentationRegistry.getContext();
  }

  @Override
  protected Context context() {
    return this.instrumentationContext;
  }

}
