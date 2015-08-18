package org.nypl.simplified.tests.files;

/**
 * The type of file locking contracts.
 */

public interface FileLockingContractType
{
  /**
   * Test that a lock can be obtained.
   *
   * @throws Exception On errors
   */

  void testLockingSimple()
    throws Exception;

  /**
   * Test that a lock on a file can be obtained by a given thread, and that if
   * the same thread cannot obtain another lock until it has released the
   * first.
   *
   * @throws Exception On errors
   */

  void testLockingSelf()
    throws Exception;

  /**
   * Test that two threads cannot obtain a lock on the same file.
   *
   * @throws Exception On errors
   */

  void testLockingOtherThread()
    throws Exception;
}
