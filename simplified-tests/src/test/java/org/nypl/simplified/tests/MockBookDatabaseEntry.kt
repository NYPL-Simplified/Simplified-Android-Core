package org.nypl.simplified.tests

import com.io7m.junreachable.UnimplementedCodeException
import one.irradia.mime.api.MIMECompatibility
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericPDFFiles
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPaths
import org.slf4j.LoggerFactory
import java.io.File

class MockBookDatabaseEntry(private val bookInitial: Book) : BookDatabaseEntryType {

  private val logger =
    LoggerFactory.getLogger(MockBookDatabaseEntry::class.java)

  val formatHandlesField: MutableList<BookDatabaseEntryFormatHandle> =
    this.formatHandlesOf(this.bookInitial.entry).toMutableList()

  var entryWrites = 0
  var entryField: OPDSAcquisitionFeedEntry = this.bookInitial.entry
  var thumbnailField: File? = null
  var deleted = false
  var coverField: File? = null

  override val book: Book
    get() = this.makeBook()

  private fun makeBook(): Book {
    return Book(
      id = this.bookInitial.id,
      account = this.bookInitial.account,
      cover = this.coverField,
      thumbnail = this.thumbnailField,
      entry = this.entryField,
      formats = this.makeFormats()
    )
  }

  private fun makeFormats(): List<BookFormat> {
    return this.formatHandles.map { handle -> handle.format }
  }

  override fun setCover(file: File) {
    this.coverField = file
  }

  override fun setThumbnail(file: File) {
    this.thumbnailField = file
  }

  override fun writeOPDSEntry(opdsEntry: OPDSAcquisitionFeedEntry) {
    this.logger.debug("[{}]: writeOPDSEntry", this.bookInitial.id)

    this.addNecessaryHandles(opdsEntry)
    this.entryField = opdsEntry
    this.entryWrites++
  }

  private fun addNecessaryHandles(opdsEntry: OPDSAcquisitionFeedEntry) {
    val handles = this.formatHandlesOf(opdsEntry)
    for (handle in handles) {
      if (this.formatHandlesField.find { it.javaClass == handle.javaClass } == null) {
        this.logger.debug("adding handle {}", handle.javaClass.name)
        this.formatHandlesField.add(handle)
      }
    }
  }

  private fun formatHandlesOf(
    entry: OPDSAcquisitionFeedEntry
  ): List<BookDatabaseEntryFormatHandle> {
    val bookId = BookIDs.newFromOPDSEntry(entry)
    val paths = OPDSAcquisitionPaths.linearize(entry)
    val finalTypes = paths.map { it.asMIMETypes().last() }
    val formats = mutableListOf<BookDatabaseEntryFormatHandle>()
    for (finalType in finalTypes) {
      if (MIMECompatibility.isCompatibleStrictWithoutAttributes(finalType, genericEPUBFiles)) {
        formats.add(MockBookDatabaseEntryFormatHandleEPUB(bookId))
        continue
      }
      if (MIMECompatibility.isCompatibleStrictWithoutAttributes(finalType, genericPDFFiles)) {
        formats.add(MockBookDatabaseEntryFormatHandlePDF(bookId))
        continue
      }
    }
    this.logger.debug("[{}]: creating {} format handles", this.bookInitial.id, formats.size)
    return formats.toList()
  }

  override fun delete() {
    this.deleted = true
  }

  override fun temporaryFile(): File {
    throw UnimplementedCodeException()
  }

  override val formatHandles: List<BookDatabaseEntryFormatHandle>
    get() = this.formatHandlesField
}
