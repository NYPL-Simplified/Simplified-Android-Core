package org.librarysimplified.documents

import android.content.res.AssetManager
import org.librarysimplified.documents.internal.DocumentStore
import org.librarysimplified.documents.internal.EmptyDocumentStore
import org.librarysimplified.http.api.LSHTTPClientType
import java.io.File

/**
 * Functions to create document stores.
 */

object DocumentStores {

  /**
   * Create a new document store.
   */

  fun create(
    assetManager: AssetManager,
    http: LSHTTPClientType,
    baseDirectory: File,
    configuration: DocumentConfigurationServiceType
  ): DocumentStoreType {
    return DocumentStore.create(
      assetManager = assetManager,
      http = http,
      baseDirectory = baseDirectory,
      configuration = configuration
    )
  }

  /**
   * Create a new document store.
   */

  fun createEmpty(): DocumentStoreType {
    return EmptyDocumentStore
  }
}
