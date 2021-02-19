package org.nypl.drm.core;

/**
 * The type of listeners that receive the results of loan return operations.
 */

public interface AdobeAdeptLoanReturnListenerType
{
  /**
   * Returning the loan succeeded.
   */

  void onLoanReturnSuccess();

  /**
   * Returning the loan failed.
   *
   * @param error The error message
   */

  void onLoanReturnFailure(String error);
}
