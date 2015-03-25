package org.nypl.simplified.books.tests.android;

import org.nypl.simplified.books.tests.contracts.BooksContract;
import org.nypl.simplified.books.tests.contracts.BooksContractType;

import android.test.InstrumentationTestCase;

public final class BooksTest extends InstrumentationTestCase implements
  BooksContractType
{
  private final BooksContract contract;

  public BooksTest()
  {
    this.contract = new BooksContract();
  }

  @Override public void testBooksLoadFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksLoadFileNotDirectory();
  }

  @Override public void testBooksLoadNotLoggedIn()
    throws Exception
  {
    this.contract.testBooksLoadNotLoggedIn();
  }

  @Override public void testBooksLoginAcceptedFirst()
    throws Exception
  {
    this.contract.testBooksLoginAcceptedFirst();
  }

  @Override public void testBooksLoginFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksLoginFileNotDirectory();
  }

  @Override public void testBooksLoginNoPINGiven()
    throws Exception
  {
    this.contract.testBooksLoginNoPINGiven();
  }

  @Override public void testBooksLoginRejectedFirstAcceptedSecond()
    throws Exception
  {
    this.contract.testBooksLoginRejectedFirstAcceptedSecond();
  }

  @Override public void testBooksLoginRejectedFirstGaveUpSecond()
    throws Exception
  {
    this.contract.testBooksLoginRejectedFirstGaveUpSecond();
  }

  @Override public void testBooksSyncFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksSyncFileNotDirectory();
  }

  @Override public void testBooksSyncLoadLogoutOK()
    throws Exception
  {
    this.contract.testBooksSyncLoadLogoutOK();
  }

  @Override public void testBooksSyncOK()
    throws Exception
  {
    this.contract.testBooksSyncOK();
  }
}
