package org.nypl.simplified.books.tests.contracts;

public interface BookDatabaseContractType
{
  void testBooksDatabaseInit()
    throws Exception;

  void testBooksDatabaseInitFailed()
    throws Exception;

  void testBooksDatabaseEntry()
    throws Exception;
}
