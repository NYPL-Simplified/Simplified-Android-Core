package org.nypl.simplified.tests.files.android;

import android.test.InstrumentationTestCase;
import org.nypl.simplified.tests.files.FileLockingContract;
import org.nypl.simplified.tests.files.FileLockingContractType;

/**
 * The Android implementation of the {@link FileLockingContractType}.
 */

public final class FilesTest extends InstrumentationTestCase
  implements FileLockingContractType
{
  private final FileLockingContractType contract;

  /**
   * Construct a test.
   */

  public FilesTest()
  {
    this.contract = new FileLockingContract();
  }

  @Override public void testLockingSimple()
    throws Exception
  {
    this.contract.testLockingSimple();
  }

  @Override public void testLockingSelf()
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
