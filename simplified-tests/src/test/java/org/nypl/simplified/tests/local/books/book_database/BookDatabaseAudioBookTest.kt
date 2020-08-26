package org.nypl.simplified.tests.local.books.book_database

import android.content.Context
import org.mockito.Mockito
import org.nypl.simplified.tests.books.book_database.BookDatabaseAudioBookContract

class BookDatabaseAudioBookTest : BookDatabaseAudioBookContract() {
  override fun context(): Context {
    return Mockito.mock(Context::class.java)
  }
}
