package org.librarysimplified.documents

/**
 * The document configuration service.
 */

interface DocumentConfigurationServiceType {

  /**
   * @return The application privacy policy, if any.
   */

  val privacyPolicy: DocumentConfiguration?

  /**
   * @return The application acknowledgements, if any.
   */

  val about: DocumentConfiguration?

  /**
   * @return The application acknowledgements, if any.
   */

  val acknowledgements: DocumentConfiguration?

  /**
   * @return The EULA, if any
   */

  val eula: DocumentConfiguration?

  /**
   * @return The application licenses, if any.
   */

  val licenses: DocumentConfiguration?
}
