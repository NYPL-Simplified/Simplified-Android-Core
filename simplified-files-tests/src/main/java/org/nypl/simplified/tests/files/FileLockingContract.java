package org.nypl.simplified.tests.files;

import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.nypl.simplified.test.utilities.TestUtilities;
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

public final class FileLockingContract implements FileLockingContractType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FileLockingContract.class));
  }

  /**
   * Construct a new contract.
   */

  public FileLockingContract()
  {

  }

  @Override public void testLockingSimple()
    throws Exception
  {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");

    final AtomicBoolean locked = new AtomicBoolean(false);

    FileLocking.withFileThreadLocked(
      lock, 1000, new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          locked.set(true);
          return Unit.unit();
        }
      });

    TestUtilities.assertTrue(locked.get());
  }

  @Override public void testLockingSelf()
    throws Exception
  {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");

    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);

    FileLockingContract.LOG.debug("attempting outer lock");
    FileLocking.withFileThreadLocked(
      lock, 1000, new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit u0)
          throws IOException
        {
          count.set(1);

          try {
            FileLockingContract.LOG.debug("attempting inner lock");
            FileLocking.withFileThreadLocked(
              lock, 1000, new PartialFunctionType<Unit, Unit, IOException>()
              {
                @Override public Unit call(
                  final Unit u1)
                  throws IOException
                {
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

    TestUtilities.assertEquals(count.get(), 2);
    TestUtilities.assertEquals(failed.get(), true);
  }

  @Override public void testLockingOtherThread()
    throws Exception
  {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File lock = new File(tmp, "lock.txt");
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicInteger count = new AtomicInteger(0);

    final Thread t1 = new Thread()
    {
      @Override public void run()
      {
        try {
          FileLocking.withFileThreadLocked(
            lock, 1000, new PartialFunctionType<Unit, Unit, IOException>()
            {
              @Override public Unit call(
                final Unit x)
                throws IOException
              {
                count.incrementAndGet();
                latch.countDown();

                try {
                  Thread.sleep(1000);
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
    latch.await(5, TimeUnit.SECONDS);

    final Thread t2 = new Thread()
    {
      @Override public void run()
      {
        try {
          FileLocking.withFileThreadLocked(
            lock, 500, new PartialFunctionType<Unit, Unit, IOException>()
            {
              @Override public Unit call(
                final Unit x)
                throws IOException
              {
                count.incrementAndGet();
                return Unit.unit();
              }
            });
        } catch (Exception e) {
          FileLockingContract.LOG.error("error: ", e);
        }
      }
    };

    TestUtilities.assertEquals(1, count.get());
  }
}
