package org.nypl.simplified.books.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.books.tests.contracts.BookIDContract;
import org.nypl.simplified.books.tests.contracts.BookIDContractType;

public final class BookTest implements BookIDContractType
{
  private final BookIDContract contract;

  public BookTest()
  {
    this.contract = new BookIDContract();
  }

  @Override @Test public void testBookIDNew()
  {
    this.contract.testBookIDNew();
  }
}
