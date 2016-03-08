package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of document stores.
 */

public interface DocumentStoreType
{
  /**
   * @return The application privacy policy, if any.
   */

  OptionType<SyncedDocumentType> getPrivacyPolicy();

  /**
   * @return The application acknowledgements, if any.
   */

  OptionType<SyncedDocumentType> getAbout();

  /**
   * @return The application acknowledgements, if any.
   */

  OptionType<SyncedDocumentType> getAcknowledgements();

  /**
   * @return The login document
   */

  AuthenticationDocumentType getAuthenticationDocument();

  /**
   * @return The EULA, if any
   */

  OptionType<EULAType> getEULA();
}
