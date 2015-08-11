package org.nypl.simplified.files;

import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Trivial file locking utilities.
 */

public final class FileLocking
{
  private static final Logger                   LOG;
  private static final Map<File, ReentrantLock> PATH_LOCKS;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FileLocking.class));
    PATH_LOCKS = new WeakHashMap<File, ReentrantLock>(16);
  }

  private FileLocking()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Attempt to acquire a lock on {@code file}, waiting for a maximum of {@code
   * milliseconds} ms. Locks are per-thread. Concurrent access to locks is not
   * prevented for separate processes. If a lock is acquired, evaluate and
   * return the result of {@code p}. If the current thread already has a lock
   * for the given file, an exception will be raised immediately without
   * waiting.
   *
   * @param file         The lock file
   * @param milliseconds The maximum wait time
   * @param p            The function to evaluate
   * @param <T>          The type of returned values
   * @param <E>          The type of thrown exceptions
   *
   * @return The value returned by {@code p}
   *
   * @throws E           If {@code p} raises {@code E}
   * @throws IOException If the lock cannot be acquired in the given time limit
   */

  public static <T, E extends Exception> T withFileThreadLocked(
    final File file,
    final long milliseconds,
    final PartialFunctionType<Unit, T, E> p)
    throws E, IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(p);

    final File f = file.getCanonicalFile();
    final ReentrantLock lock = FileLocking.getFileLock(file);
    try {
      if (lock.isHeldByCurrentThread()) {
        throw new IOException(
          String.format("Lock of file %s already held by this thread", f));
      }

      if (lock.tryLock(milliseconds, TimeUnit.MILLISECONDS)) {
        try {
          FileLocking.LOG.trace("lock obtain {}", file);
          return p.call(Unit.unit());
        } finally {
          FileLocking.LOG.trace("lock unlock {}", file);
          lock.unlock();
        }
      } else {
        throw new IOException(
          String.format(
            "Timed out waiting for lock of file %s", f));
      }
    } catch (final InterruptedException e) {
      throw new IOException(
        String.format("Interrupted waiting for lock of file %s", f));
    }
  }

  private static synchronized ReentrantLock getFileLock(final File file)
  {
    FileLocking.LOG.trace("lock request {}", file);

    final ReentrantLock lock = FileLocking.PATH_LOCKS.get(file);
    if (lock != null) {
      FileLocking.LOG.trace("lock reuse {}", file);
      return lock;
    }

    FileLocking.LOG.trace("lock new {}", file);
    final ReentrantLock new_lock = new ReentrantLock(true);
    FileLocking.PATH_LOCKS.put(file, new_lock);
    return new_lock;
  }
}
