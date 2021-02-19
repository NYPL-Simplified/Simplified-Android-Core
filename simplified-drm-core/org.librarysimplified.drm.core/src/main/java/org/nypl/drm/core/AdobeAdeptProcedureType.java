package org.nypl.drm.core;

/**
 * A procedure executed with an {@link AdobeAdeptConnectorType}.
 */

public interface AdobeAdeptProcedureType
{
  /**
   * Execute the procedure, using the connector {@code c}.
   *
   * @param c The connector
   */

  void executeWith(AdobeAdeptConnectorType c);
}
