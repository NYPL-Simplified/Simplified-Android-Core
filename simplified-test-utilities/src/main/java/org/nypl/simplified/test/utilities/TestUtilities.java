package org.nypl.simplified.test.utilities;

import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;

public final class TestUtilities
{
  private static final Object z = null;

  public static <A> void assertEquals(
    final A x,
    final A y)
  {
    if (x.equals(y) == false) {
      throw new AssertionError(String.format("%s != %s", x, y));
    }
  }

  @SuppressWarnings("boxing") public static void assertGreater(
    final int x,
    final int y)
  {
    if (x <= y) {
      throw new AssertionError(String.format("%d <= %d", x, y));
    }
  }

  public static void assertTrue(
    final boolean c)
  {
    if (c == false) {
      throw new AssertionError(c + " is unexpectedly false");
    }
  }

  public static void expectException(
    final Class<? extends Exception> c,
    final PartialProcedureType<Unit, Exception> p)
    throws Exception
  {
    boolean raised = false;
    try {
      p.call(Unit.unit());
    } catch (final Exception e) {
      if (e.getClass().equals(c)) {
        System.err.println("Caught expected exception: " + e);
        raised = true;
      } else {
        System.err.println("Unexpected exception: " + e);
        throw e;
      }
    }

    if (raised == false) {
      throw new AssertionError("Failed to raise an exception of type: " + c);
    }
  }

  /**
   * A function that will return null whilst convincing the type system that
   * it actually doesn't.
   *
   * @param <A>
   *          The type of returned value
   * @return {@code null}
   */

  @SuppressWarnings({ "null", "unchecked" }) public static @NonNull
    <A>
    A
    unexpectedlyNull()
  {
    return (A) TestUtilities.z;
  }
}
