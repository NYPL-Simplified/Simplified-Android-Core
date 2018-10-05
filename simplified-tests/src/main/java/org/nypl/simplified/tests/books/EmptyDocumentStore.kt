package org.nypl.simplified.tests.books

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType

import org.nypl.simplified.books.core.AuthenticationDocumentType
import org.nypl.simplified.books.core.DocumentStoreType
import org.nypl.simplified.books.core.EULAType
import org.nypl.simplified.books.core.SyncedDocumentType

import java.io.InputStream

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

  override fun getAuthenticationDocument(): AuthenticationDocumentType {
    return object : AuthenticationDocumentType {
      override fun getLabelLoginUserID(): String {
        return "Login"
      }

      override fun getLabelLoginPassword(): String {
        return "Password"
      }

      override fun getLabelLoginPatronName(): String {
        return "Name"
      }

      override fun documentUpdate(data: InputStream) {
        // Nothing
      }
    }
  }

  override fun getEULA(): OptionType<EULAType> {
    return Option.none()
  }

  override fun getLicenses(): OptionType<SyncedDocumentType> {
    return Option.none()
  }
}
