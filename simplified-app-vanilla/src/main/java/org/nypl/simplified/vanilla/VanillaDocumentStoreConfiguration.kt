package org.nypl.simplified.vanilla

import org.librarysimplified.documents.DocumentConfiguration
import org.librarysimplified.documents.DocumentConfigurationServiceType
import java.net.URI

class VanillaDocumentStoreConfiguration : DocumentConfigurationServiceType {

  override val privacyPolicy: DocumentConfiguration? =
    null

  override val about: DocumentConfiguration? =
    DocumentConfiguration(
      name = "about.html",
      remoteURI = URI.create("http://localhost/about.html")
    )

  override val acknowledgements: DocumentConfiguration? =
    null

  override val eula: DocumentConfiguration? =
    null

  override val licenses: DocumentConfiguration? =
    null
}
