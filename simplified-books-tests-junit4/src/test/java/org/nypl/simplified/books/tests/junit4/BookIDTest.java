package org.nypl.simplified.books.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.books.tests.contracts.BookIDContract;
import org.nypl.simplified.books.tests.contracts.BookIDContractType;

public final class BookIDTest implements BookIDContractType
{
  private final BookIDContract contract;

  public BookIDTest()
  {
    this.contract = new BookIDContract();
  }

  @Override @Test public void testBookIDNew()
  {
    this.contract.testBookIDNew();
  }

  @Override @Test public void testBookID_0()
  {
    this.contract.testBookID_0();
  }
}
