package org.nypl.simplified.tests

import org.librarysimplified.audiobook.api.PlayerPosition
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.formats.api.StandardFormatNames
import java.io.File
import java.net.URI

class MockBookDatabaseEntryFormatHandleAudioBook(
  val bookID: BookID
) : BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook() {

  var bookData: String? = null
  private var bookFile: File? = null

  var formatField: BookFormat.BookFormatAudioBook =
    BookFormat.BookFormatAudioBook(
      drmInformation = BookDRMInformation.None,
      contentType = StandardFormatNames.genericAudioBooks.first(),
      manifest = null,
      position = null
    )

  var drmInformationHandleField: BookDRMInformationHandle =
    object : BookDRMInformationHandle.NoneHandle() {
      override val info: BookDRMInformation.None =
        BookDRMInformation.None
    }

  override val format: BookFormat.BookFormatAudioBook
    get() = this.formatField

  override fun copyInManifestAndURI(data: ByteArray, manifestURI: URI) {
    this.formatField = this.formatField.copy(
      manifest = BookFormat.AudioBookManifestReference(
        manifestURI,
        File("whatever")
      )
    )
  }

  override fun savePlayerPosition(position: PlayerPosition) {
    this.formatField = this.formatField.copy(
      position = position
    )
  }

  override fun clearPlayerPosition() {
    this.formatField = this.formatField.copy(
      position = null
    )
  }

  override val drmInformationHandle: BookDRMInformationHandle
    get() = this.drmInformationHandleField

  override fun setDRMKind(kind: BookDRMKind) {
  }

  override fun deleteBookData() {
    this.bookData = null
    this.bookFile = null
    this.formatField = this.formatField.copy(manifest = null)
  }
}
