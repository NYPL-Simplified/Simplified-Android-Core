package org.nypl.simplified.documents.eula;

import org.nypl.simplified.documents.synced.SyncedDocumentType;

/**
 * The EULA interface.
 */

public interface EULAType extends SyncedDocumentType
{
  /**
   * @return {@code true} iff the user has already agreed to the EULA.
   */

  boolean eulaHasAgreed();

  /**
   * Set whether or not the user has agreed to the EULA.
   *
   * @param t {@code true} iff the user has agreed.
   */

  void eulaSetHasAgreed(boolean t);
}
