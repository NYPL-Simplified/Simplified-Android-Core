package org.nypl.simplified.books.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.books.tests.contracts.FileLockingContract;
import org.nypl.simplified.books.tests.contracts.FileLockingContractType;

public final class FileLockingTest implements FileLockingContractType
{
  private final FileLockingContract contract;

  public FileLockingTest()
  {
    this.contract = new FileLockingContract();
  }

  @Override @Test public void testLocking0()
    throws Exception
  {
    this.contract.testLocking0();
  }

  @Override @Test public void testLocking1()
    throws Exception
  {
    this.contract.testLocking1();
  }
}
