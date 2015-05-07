package org.nypl.simplified.tests.files.android;

import org.nypl.simplified.tests.files.FileLockingContract;
import org.nypl.simplified.tests.files.FileLockingContractType;

import android.test.InstrumentationTestCase;

public final class FilesTest extends InstrumentationTestCase implements
  FileLockingContractType
{
  private final FileLockingContractType contract;

  public FilesTest()
  {
    this.contract = new FileLockingContract();
  }

  @Override public void testLocking0()
    throws Exception
  {
    this.contract.testLocking0();
  }

  @Override public void testLocking1()
    throws Exception
  {
    this.contract.testLocking1();
  }
}
