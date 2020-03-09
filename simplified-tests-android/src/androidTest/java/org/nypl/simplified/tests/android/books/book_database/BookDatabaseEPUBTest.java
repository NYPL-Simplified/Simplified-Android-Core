package org.nypl.simplified.tests.android.books.book_database;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nypl.simplified.tests.books.book_database.BookDatabaseEPUBContract;

@RunWith(AndroidJUnit4.class)
@MediumTest
public final class BookDatabaseEPUBTest extends BookDatabaseEPUBContract {

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
