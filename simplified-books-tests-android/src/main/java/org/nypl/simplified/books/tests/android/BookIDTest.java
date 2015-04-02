package org.nypl.simplified.books.tests.android;

import org.nypl.simplified.books.tests.contracts.BookIDContract;
import org.nypl.simplified.books.tests.contracts.BookIDContractType;

import android.test.InstrumentationTestCase;

public final class BookIDTest extends InstrumentationTestCase implements
  BookIDContractType
{
  private final BookIDContractType contract;

  public BookIDTest()
  {
    this.contract = new BookIDContract();
  }

  @Override public void testBookIDNew()
  {
    this.contract.testBookIDNew();
  }

  @Override public void testBookID_0()
  {
    this.contract.testBookID_0();
  }
}
