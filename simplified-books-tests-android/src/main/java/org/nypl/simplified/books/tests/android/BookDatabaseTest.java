package org.nypl.simplified.books.tests.android;

import org.nypl.simplified.books.tests.contracts.BookDatabaseContract;
import org.nypl.simplified.books.tests.contracts.BookDatabaseContractType;

import android.test.InstrumentationTestCase;

public final class BookDatabaseTest extends InstrumentationTestCase implements
  BookDatabaseContractType
{
  private final BookDatabaseContractType contract;

  public BookDatabaseTest()
  {
    this.contract = new BookDatabaseContract();
  }

  @Override public void testBooksDatabaseEntry()
    throws Exception
  {
    this.contract.testBooksDatabaseEntry();
  }

  @Override public void testBooksDatabaseInit()
    throws Exception
  {
    this.contract.testBooksDatabaseInit();
  }

  @Override public void testBooksDatabaseInitFailed()
    throws Exception
  {
    this.contract.testBooksDatabaseInitFailed();
  }
}
