package org.nypl.simplified.files;

import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trivial file locking utilities.
 */

public final class FileLocking
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FileLocking.class));
  }

  private FileLocking()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Attempt to acquire a lock on <tt>file</tt>, waiting for a maximum of
   * <tt>milliseconds</tt> ms, in <tt>wait</tt> millisecond increments. If a
   * lock is acquired, evaluate and return the result of <tt>p</tt>.
   *
   * @param file         The lock file
   * @param wait         The wait increment
   * @param milliseconds The maximum wait time
   * @param p            The function to evaluate
   * @param <T>          The type of returned values
   * @param <E>          The type of thrown exceptions
   *
   * @return The value returned by <tt>p</tt>
   *
   * @throws E           If <tt>p</tt> raises <tt>E</tt>
   * @throws IOException If the lock cannot be acquired in the given time limit
   */

  public static <T, E extends Exception> T withFileLocked(
    final File file,
    final long wait,
    final long milliseconds,
    final PartialFunctionType<Unit, T, E> p)
    throws E, IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(p);

    final AtomicBoolean abort = new AtomicBoolean(false);
    final long time_start =
      TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);

    while (abort.get() == false) {
      final long time_now =
        TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);

      final long diff = time_now - time_start;
      FileLocking.LOG.trace("lock try {} (diff {})", file, Long.valueOf(diff));

      if (diff > milliseconds) {
        abort.set(true);
        FileLocking.LOG.trace("lock timeout {} ", file);
        break;
      }

      try {
        final FileOutputStream fs = new FileOutputStream(file);
        try {
          final FileChannel fc = fs.getChannel();
          try {
            final FileLock lock = fc.lock();
            try {
              FileLocking.LOG.trace("lock obtain {}", file);
              return p.call(Unit.unit());
            } finally {
              lock.release();
              FileLocking.LOG.trace("lock release {}", file);
            }
          } finally {
            fc.close();
          }
        } finally {
          fs.close();
        }
      } catch (final OverlappingFileLockException e) {
        try {
          Thread.sleep(wait);
        } catch (final InterruptedException x) {
          // Don't care
        }
      }
    }

    final String m = String.format(
      "Timed out attempting to lock '%s' after %d milliseconds",
      file,
      Long.valueOf(milliseconds));
    throw new IOException(m);
  }
}
