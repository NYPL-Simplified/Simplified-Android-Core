package org.nypl.simplified.tests.files.junit4;

import org.junit.Test;
import org.nypl.simplified.tests.files.FileLockingContract;
import org.nypl.simplified.tests.files.FileLockingContractType;

public final class FilesTest implements FileLockingContractType
{
  private final FileLockingContract contract;

  public FilesTest()
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
