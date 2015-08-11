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

  @Override @Test public void testLockingSimple()
    throws Exception
  {
    this.contract.testLockingSimple();
  }

  @Override @Test public void testLockingSelf()
    throws Exception
  {
    this.contract.testLockingSelf();
  }

  @Override public void testLockingOtherThread()
    throws Exception
  {
    this.contract.testLockingOtherThread();
  }
}
