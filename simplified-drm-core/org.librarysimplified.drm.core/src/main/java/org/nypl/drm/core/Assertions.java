package org.nypl.drm.core;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trivial class containing functions for checking preconditions and
 * invariants ("assertions that cannot be turned off").
 */

public final class Assertions
{
  private static final Logger LOG;

  static {
    LOG = Objects.requireNonNull(LoggerFactory.getLogger(Assertions.class));
  }

  private Assertions()
  {
    throw new AssertionError("Unreachable code!");
  }

  /**
   * Require that the given invariant <tt>c</tt> be <tt>true</tt>, raising
   * {@link AssertionError} if it isn't.
   *
   * @param condition The invariant
   * @param message   The message displayed on failure (a format string)
   * @param args      The message format arguments
   */

  public static void checkInvariant(
    final boolean condition,
    final String message,
    final Object... args)
  {
    if (condition == false) {
      final String m = String.format(message, args);
      Assertions.LOG.error("assertion failed: invariant: {}", m);
      throw new AssertionError(m);
    }
  }

  /**
   * Require that the given precondition <tt>c</tt> be <tt>true</tt>, raising
   * {@link AssertionError} if it isn't.
   *
   * @param c       The precondition
   * @param message The message displayed on failure (a format string)
   * @param args    The message format arguments
   */

  public static void checkPrecondition(
    final boolean c,
    final String message,
    final Object... args)
  {
    if (c == false) {
      final String m = String.format(message, args);
      Assertions.LOG.error("assertion failed: precondition: {}", m);
      throw new AssertionError(m);
    }
  }
}
