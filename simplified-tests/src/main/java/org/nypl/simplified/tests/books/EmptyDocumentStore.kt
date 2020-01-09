package org.nypl.simplified.tests.books

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.documents.synced.SyncedDocumentType

/**
 * A document store implementation that contains no documents.
 */

open class EmptyDocumentStore : DocumentStoreType {

  override fun getPrivacyPolicy(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getAcknowledgements(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getAbout(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getEULA(): OptionType<EULAType> {
    return Option.none()
  }

  override fun getLicenses(): OptionType<SyncedDocumentType> {
    return Option.none()
  }
}
