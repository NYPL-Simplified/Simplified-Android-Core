package org.nypl.simplified.books.tests.junit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.nypl.simplified.books.tests.contracts.BookDatabaseContract;
import org.nypl.simplified.books.tests.contracts.BookDatabaseContractType;

public final class BookDatabaseTest implements BookDatabaseContractType
{
  private final BookDatabaseContract contract;

  @Rule public Timeout               globalTimeout = new Timeout(10000);

  public BookDatabaseTest()
  {
    this.contract = new BookDatabaseContract();
  }

  @Override @Test public void testBooksDatabaseEntry()
    throws Exception
  {
    this.contract.testBooksDatabaseEntry();
  }

  @Override @Test public void testBooksDatabaseInit()
    throws Exception
  {
    this.contract.testBooksDatabaseInit();
  }

  @Override @Test public void testBooksDatabaseInitFailed()
    throws Exception
  {
    this.contract.testBooksDatabaseInitFailed();
  }
}
