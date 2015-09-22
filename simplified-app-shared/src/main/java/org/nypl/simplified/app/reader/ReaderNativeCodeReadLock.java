package org.nypl.simplified.app.reader;

/**
 * Because various pieces of native code are not thread safe, it is necessary to
 * protect access to it with a critical section at very fine granularity. The
 * {@code ReaderNativeCodeReadLock} class represents a singleton read lock that
 * can be used with a {@code synchronized} statement. The type specifically
 * exists to make it clear that this is what it is for, as opposed to passing
 * around a raw {@code java.lang.Object}.
 */

public final class ReaderNativeCodeReadLock
{
  private static final ReaderNativeCodeReadLock INSTANCE;

  static {
    INSTANCE = new ReaderNativeCodeReadLock();
  }

  private ReaderNativeCodeReadLock()
  {

  }

  /**
   * @return The lock instance
   */

  public static ReaderNativeCodeReadLock get()
  {
    return ReaderNativeCodeReadLock.INSTANCE;
  }
}
