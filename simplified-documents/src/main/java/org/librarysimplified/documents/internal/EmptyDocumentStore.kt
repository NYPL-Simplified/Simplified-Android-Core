package org.librarysimplified.documents.internal

import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.documents.DocumentType
import org.librarysimplified.documents.EULAType

internal object EmptyDocumentStore : DocumentStoreType {
  override val privacyPolicy: DocumentType? =
    null
  override val about: DocumentType? =
    null
  override val acknowledgements: DocumentType? =
    null
  override val eula: EULAType? =
    null
  override val licenses: DocumentType? =
    null
}
