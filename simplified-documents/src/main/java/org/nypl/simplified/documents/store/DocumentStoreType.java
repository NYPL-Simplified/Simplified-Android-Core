package org.nypl.simplified.documents.store;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.documents.authentication.AuthenticationDocumentType;
import org.nypl.simplified.documents.eula.EULAType;
import org.nypl.simplified.documents.synced.SyncedDocumentType;

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

  /**
   * @return The application licenses, if any.
   */

  OptionType<SyncedDocumentType> getLicenses();

}
