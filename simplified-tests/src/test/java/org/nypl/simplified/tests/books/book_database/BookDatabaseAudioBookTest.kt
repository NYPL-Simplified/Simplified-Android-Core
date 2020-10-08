package org.nypl.simplified.tests.books.book_database

import android.content.Context
import org.mockito.Mockito

class BookDatabaseAudioBookTest : BookDatabaseAudioBookContract() {
  override fun context(): Context {
    return Mockito.mock(Context::class.java)
  }
}
