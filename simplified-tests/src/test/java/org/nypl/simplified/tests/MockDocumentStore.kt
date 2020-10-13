package org.nypl.simplified.tests

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.documents.synced.SyncedDocumentType
import java.net.URL

class MockDocumentStore : DocumentStoreType {
  override fun getPrivacyPolicy(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getAbout(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getAcknowledgements(): OptionType<SyncedDocumentType> {
    return Option.none()
  }

  override fun getEULA(): OptionType<EULAType> {
    return Option.of(
      object : EULAType {
        var agreed = true

        override fun eulaHasAgreed(): Boolean {
          return agreed
        }

        override fun documentGetReadableURL(): URL {
          return URL("http://www.example.com")
        }

        override fun documentSetLatestURL(u: URL?) {
        }

        override fun eulaSetHasAgreed(t: Boolean) {
          agreed = t
        }
      }
    )
  }

  override fun getLicenses(): OptionType<SyncedDocumentType> {
    return Option.none()
  }
}
