package org.nypl.simplified.simplye

import org.librarysimplified.documents.DocumentConfiguration
import org.librarysimplified.documents.DocumentConfigurationServiceType
import java.net.URI

class SimplyEDocumentConfiguration : DocumentConfigurationServiceType {

  override val about: DocumentConfiguration? =
    null

  override val acknowledgements: DocumentConfiguration? =
    null

  override val eula: DocumentConfiguration? =
    DocumentConfiguration(
      "eula.html",
      URI.create("http://localhost/eula.html")
    )

  override val licenses: DocumentConfiguration? =
    DocumentConfiguration(
      "software-licenses.html",
      URI.create("http://localhost/software-licenses.html")
    )

  override val privacyPolicy: DocumentConfiguration? =
    null
}
