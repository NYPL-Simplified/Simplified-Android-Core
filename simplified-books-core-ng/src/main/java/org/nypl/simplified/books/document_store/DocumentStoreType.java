package org.nypl.simplified.books.document_store;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.books.authentication_document.AuthenticationDocumentType;
import org.nypl.simplified.books.eula.EULAType;
import org.nypl.simplified.books.synced_document.SyncedDocumentType;

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
