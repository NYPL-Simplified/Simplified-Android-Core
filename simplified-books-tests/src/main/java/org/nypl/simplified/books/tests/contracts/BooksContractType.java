package org.nypl.simplified.books.tests.contracts;

public interface BooksContractType
{
  void testBooksLoadNotLoggedIn()
    throws Exception;

  void testBooksLoginAcceptedFirst()
    throws Exception;

  void testBooksLoadFileNotDirectory()
    throws Exception;

  void testBooksLoginFileNotDirectory()
    throws Exception;

  void testBooksSyncOK()
    throws Exception;

  void testBooksSyncLoadLogoutOK()
    throws Exception;
}
