package org.librarysimplified.documents

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService

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

  /**
   * Run updates for all of the documents.
   */

  fun update(executor: ListeningExecutorService): ListenableFuture<*>
}
