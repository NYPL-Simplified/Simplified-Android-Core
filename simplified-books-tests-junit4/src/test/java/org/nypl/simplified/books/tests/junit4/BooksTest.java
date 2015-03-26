package org.nypl.simplified.books.tests.junit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.nypl.simplified.books.tests.contracts.BooksContract;
import org.nypl.simplified.books.tests.contracts.BooksContractType;

public final class BooksTest implements BooksContractType
{
  private final BooksContract contract;

  @Rule public Timeout        globalTimeout = new Timeout(10000);

  public BooksTest()
  {
    this.contract = new BooksContract();
  }

  @Override @Test public void testBooksLoadFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksLoadFileNotDirectory();
  }

  @Override @Test public void testBooksLoadNotLoggedIn()
    throws Exception
  {
    this.contract.testBooksLoadNotLoggedIn();
  }

  @Override @Test public void testBooksLoginAcceptedFirst()
    throws Exception
  {
    this.contract.testBooksLoginAcceptedFirst();
  }

  @Override @Test public void testBooksLoginFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksLoginFileNotDirectory();
  }

  @Override @Test public void testBooksSyncFileNotDirectory()
    throws Exception
  {
    this.contract.testBooksSyncFileNotDirectory();
  }

  @Override @Test public void testBooksSyncLoadLogoutOK()
    throws Exception
  {
    this.contract.testBooksSyncLoadLogoutOK();
  }

  @Override @Test public void testBooksSyncOK()
    throws Exception
  {
    this.contract.testBooksSyncOK();
  }
}
