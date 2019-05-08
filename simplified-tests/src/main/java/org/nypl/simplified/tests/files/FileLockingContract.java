package org.nypl.simplified.tests.files;

import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main file locking contract.
 */

public abstract class FileLockingContract {

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FileLockingContract.class));
  }

  /**
   * Construct a new contract.
   */

  public FileLockingContract() {

  }

  /**
   * Test that a lock can be obtained.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLockingSimple()
      throws Exception {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");

    final AtomicBoolean locked = new AtomicBoolean(false);

    FileLocking.withFileThreadLocked(
        lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>() {
          @Override
          public Unit call(
              final Unit x)
              throws IOException {
            locked.set(true);
            return Unit.unit();
          }
        });

    Assert.assertTrue(locked.get());
  }

  /**
   * Test that a lock on a file can be obtained by a given thread, and that if
   * the same thread cannot obtain another lock until it has released the
   * first.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLockingSelf()
      throws Exception {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");

    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);

    FileLockingContract.LOG.debug("attempting outer lock");
    FileLocking.withFileThreadLocked(
        lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>() {
          @Override
          public Unit call(
              final Unit u0)
              throws IOException {
            count.set(1);

            try {
              FileLockingContract.LOG.debug("attempting inner lock");
              FileLocking.withFileThreadLocked(
                  lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>() {
                    @Override
                    public Unit call(
                        final Unit u1)
                        throws IOException {
                      FileLockingContract.LOG.debug("called inner lock");
                      count.set(3);
                      return Unit.unit();
                    }
                  });
            } catch (final IOException e) {
              FileLockingContract.LOG.error("io error: ", e);
              failed.set(true);
            }

            FileLockingContract.LOG.debug("finished inner lock");
            count.set(2);
            return Unit.unit();
          }
        });
    FileLockingContract.LOG.debug("finished outer lock");

    Assert.assertEquals(Integer.valueOf(count.get()), Integer.valueOf(2));
    Assert.assertEquals(Boolean.valueOf(failed.get()), Boolean.TRUE);
  }

  /**
   * Test that two threads cannot obtain a lock on the same file.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLockingOtherThread()
      throws Exception {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicInteger count = new AtomicInteger(0);

    final Thread t1 = new Thread() {
      @Override
      public void run() {
        try {
          FileLocking.withFileThreadLocked(
              lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>() {
                @Override
                public Unit call(
                    final Unit x)
                    throws IOException {
                  count.incrementAndGet();
                  latch.countDown();

                  try {
                    Thread.sleep(1000L);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  return Unit.unit();
                }
              });
        } catch (final Exception e) {
          FileLockingContract.LOG.error("error: ", e);
        }
      }
    };

    t1.start();
    latch.await(5L, TimeUnit.SECONDS);

    final Thread t2 = new Thread() {
      @Override
      public void run() {
        try {
          FileLocking.withFileThreadLocked(
              lock, 500L, new PartialFunctionType<Unit, Unit, IOException>() {
                @Override
                public Unit call(
                    final Unit x)
                    throws IOException {
                  count.incrementAndGet();
                  return Unit.unit();
                }
              });
        } catch (Exception e) {
          FileLockingContract.LOG.error("error: ", e);
        }
      }
    };

    Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(count.get()));
  }
}
