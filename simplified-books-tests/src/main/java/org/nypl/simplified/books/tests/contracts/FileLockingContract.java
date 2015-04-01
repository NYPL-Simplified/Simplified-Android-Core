package org.nypl.simplified.books.tests.contracts;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.books.core.FileLocking;
import org.nypl.simplified.test.utilities.TestUtilities;

import com.google.common.io.Files;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;

public final class FileLockingContract implements FileLockingContractType
{
  public FileLockingContract()
  {

  }

  @Override public void testLocking0()
    throws Exception
  {
    final File tmp = Files.createTempDir();
    tmp.mkdirs();
    final File lock = new File(tmp, "lock.txt");
    final AtomicBoolean locked = new AtomicBoolean(false);

    FileLocking.withFileLocked(
      lock,
      10,
      1000,
      new PartialFunctionType<Unit, Unit, IOException>() {
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

  @Override public void testLocking1()
    throws Exception
  {
    final File tmp = Files.createTempDir();
    tmp.mkdirs();
    final File lock = new File(tmp, "lock.txt");

    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);

    FileLocking.withFileLocked(
      lock,
      10,
      1000,
      new PartialFunctionType<Unit, Unit, IOException>() {
        @Override public Unit call(
          final Unit u0)
          throws IOException
        {
          count.set(1);

          try {
            FileLocking.withFileLocked(
              lock,
              1,
              10,
              new PartialFunctionType<Unit, Unit, IOException>() {
                @Override public Unit call(
                  final Unit u1)
                  throws IOException
                {
                  count.set(3);
                  return Unit.unit();
                }
              });
          } catch (final IOException e) {
            failed.set(true);
          }

          count.set(2);
          return Unit.unit();
        }
      });

    TestUtilities.assertEquals(count.get(), 2);
    TestUtilities.assertEquals(failed.get(), true);
  }
}
