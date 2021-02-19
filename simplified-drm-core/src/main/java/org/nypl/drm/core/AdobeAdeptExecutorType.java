package org.nypl.drm.core;

/**
 * An executor that executes procedures on a single background thread in order
 * to preserve all the required threading invariants of the Adept Connector.
 */

public interface AdobeAdeptExecutorType
{
  /**
   * Execute the given procedure. Execution is asynchronous and this method
   * returns immediately.
   *
   * @param p The procedure
   */

  void execute(AdobeAdeptProcedureType p);
}
