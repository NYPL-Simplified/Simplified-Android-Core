package org.librarysimplified.documents

interface DocumentStoreType {

  /**
   * @return The application privacy policy, if any.
   */

  val privacyPolicy: DocumentType?

  /**
   * @return The application acknowledgements, if any.
   */

  val about: DocumentType?

  /**
   * @return The application acknowledgements, if any.
   */

  val acknowledgements: DocumentType?

  /**
   * @return The EULA, if any
   */

  val eula: EULAType?

  /**
   * @return The application licenses, if any.
   */

  val licenses: DocumentType?
}
