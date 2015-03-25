package org.nypl.simplified.books.tests.contracts;

public interface BooksContractType
{
  void testBooksLoadNotLoggedIn()
    throws Exception;

  void testBooksLoginRejectedFirstAcceptedSecond()
    throws Exception;

  void testBooksLoginAcceptedFirst()
    throws Exception;

  void testBooksLoginNoPINGiven()
    throws Exception;

  void testBooksLoadFileNotDirectory()
    throws Exception;

  void testBooksLoginFileNotDirectory()
    throws Exception;

  void testBooksSyncFileNotDirectory()
    throws Exception;

  void testBooksSyncOK()
    throws Exception;

  void testBooksSyncLoadLogoutOK()
    throws Exception;

  void testBooksLoginRejectedFirstGaveUpSecond()
    throws Exception;
}
