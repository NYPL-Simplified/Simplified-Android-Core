package org.nypl.simplified.app;

/**
 * Expensive operations that can be explicitly <i>started</i>.
 *
 * @see ExpensiveType
 */

public interface ExpensiveStartableType
{
  /**
   * Start doing whatever long-running operation this type represents. This
   * function is expected to be idempotent with respect to itself.
   */

  void expensiveStart();
}
