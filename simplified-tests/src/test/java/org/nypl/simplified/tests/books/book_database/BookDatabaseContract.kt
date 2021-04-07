package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.librarysimplified.audiobook.api.PlayerPosition
import org.nypl.simplified.books.api.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.api.BookFormat.BookFormatPDF
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.UUID

abstract class BookDatabaseContract {

  private val logger =
    LoggerFactory.getLogger(BookDatabaseContract::class.java)
  private val accountID =
    org.nypl.simplified.accounts.api.AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract fun context(): Context

  /**
   * Opening an empty database works.
   */

  @Test
  fun testOpenEmpty() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()

    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database =
      BookDatabase.open(context(), parser, serializer, accountID, directory)
    Assertions.assertEquals(0L, database.books().size.toLong())
  }

  /**
   * Creating and reopening an empty database works.
   */

  @Test
  fun testOpenCreateReopen() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val entry0 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none<URI>())
      )
        .build()

    val entry1 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "b",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none<URI>())
      )
        .build()

    val entry2 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "c",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none<URI>())
      )
        .build()

    val id0 = org.nypl.simplified.books.api.BookID.create("a")
    database0.createOrUpdate(id0, entry0)
    val id1 = org.nypl.simplified.books.api.BookID.create("b")
    database0.createOrUpdate(id1, entry1)
    val id2 = org.nypl.simplified.books.api.BookID.create("c")
    database0.createOrUpdate(id2, entry2)

    val database1 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    Assertions.assertEquals(3, database1.books().size.toLong())
    Assertions.assertTrue(database1.books().contains(id0))
    Assertions.assertTrue(database1.books().contains(id1))
    Assertions.assertTrue(database1.books().contains(id2))
    Assertions.assertEquals(database1.entry(id0).book.id.value(), entry0.id)
    Assertions.assertEquals(database1.entry(id1).book.id.value(), entry1.id)
    Assertions.assertEquals(database1.entry(id2).book.id.value(), entry2.id)
  }

  /**
   * Creating and deleting a database entry works.
   */

  @Test
  fun testOpenCreateDelete() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()

    val directory = DirectoryUtilities.directoryCreateTemporary()
    val db0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val entry0 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none<URI>())
      )
        .build()

    val id0 = org.nypl.simplified.books.api.BookID.create("a")
    val dbEntry = db0.createOrUpdate(id0, entry0)
    Assertions.assertEquals(1, db0.books().size.toLong())
    dbEntry.delete()
    Assertions.assertEquals(0, db0.books().size.toLong())
  }

  /**
   * Creating and reopening an empty database works.
   */

  @Test
  fun testEntrySetCover() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val book0 = databaseEntry0.book
    Assertions.assertEquals(null, book0.cover)

    databaseEntry0.setCover(copyToTempFile("/org/nypl/simplified/tests/books/empty.jpg"))
    val book1 = databaseEntry0.book
    val cover = book1.cover!!
    Assertions.assertTrue(cover.isFile)
  }

  /**
   * Creating a book database entry for a feed that contains an EPUB acquisition results in an
   * EPUB format. Reopening the database shows that the data is preserved.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryHasEPUBFormat() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val book0: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle0 =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assertions.assertTrue(formatHandle0 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry0, BookDatabaseEntryFormatHandleEPUB::class.java
      )

      val epubFormat = databaseEntry0.book.findFormat(BookFormatEPUB::class.java)
      Assertions.assertTrue(epubFormat != null, "Format is present")

      epubFormat!!
      Assertions.assertTrue(epubFormat.file == null, "No book data")
      Assertions.assertFalse(epubFormat.isDownloaded, "Book is not downloaded")

      Assertions.assertEquals(
        formatHandle0,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/epub+zip"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry0.book
    }

    val database1 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)
    val databaseEntry1 = database1.entry(bookID)

    val book1: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle1 =
        databaseEntry1.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assertions.assertTrue(formatHandle1 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry1, BookDatabaseEntryFormatHandleEPUB::class.java
      )

      val epubFormat = databaseEntry1.book.findFormat(BookFormatEPUB::class.java)
      Assertions.assertTrue(epubFormat != null, "Format is present")

      epubFormat!!
      Assertions.assertTrue(epubFormat.file == null, "No book data")
      Assertions.assertFalse(epubFormat.isDownloaded, "Book is not downloaded")

      Assertions.assertEquals(
        formatHandle1,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/epub+zip"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry1.book
    }

    this.compareBooks(book0, book1)
  }

  /**
   * Creating a book database entry for a feed that contains a PDF acquisition results in an
   * PDF format.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryHasPDFFormat() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithPDF()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val book0: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle0 =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)

      Assertions.assertTrue(formatHandle0 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry0, BookDatabaseEntryFormatHandlePDF::class.java
      )

      val pdfFormat = databaseEntry0.book.findFormat(BookFormatPDF::class.java)
      Assertions.assertTrue(pdfFormat != null, "Format is present")

      pdfFormat!!
      Assertions.assertTrue(pdfFormat.file == null, "No book data")
      Assertions.assertFalse(pdfFormat.isDownloaded, "Book is not downloaded")

      Assertions.assertEquals(
        formatHandle0,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/pdf"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry0.book
    }

    val database1 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)
    val databaseEntry1 = database1.entry(bookID)

    val book1: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle1 =
        databaseEntry1.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)

      Assertions.assertTrue(formatHandle1 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry1, BookDatabaseEntryFormatHandlePDF::class.java
      )

      val pdfFormat = databaseEntry1.book.findFormat(BookFormatPDF::class.java)
      Assertions.assertTrue(pdfFormat != null, "Format is present")

      pdfFormat!!
      Assertions.assertTrue(pdfFormat.file == null, "No book data")
      Assertions.assertFalse(pdfFormat.isDownloaded, "Book is not downloaded")

      Assertions.assertEquals(
        formatHandle1,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/pdf"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry1.book
    }

    this.compareBooks(book0, book1)
  }

  /**
   * Creating a book database entry for a feed that contains an audio book acquisition results in an
   * audio book format.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryHasAudioBookFormat() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithAudioBook()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val book0: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle0 =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

      Assertions.assertTrue(formatHandle0 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry0, BookDatabaseEntryFormatHandleAudioBook::class.java
      )

      val audioFormat = databaseEntry0.book.findFormat(BookFormatAudioBook::class.java)
      Assertions.assertTrue(audioFormat != null, "Format is present")

      audioFormat!!
      Assertions.assertTrue(audioFormat.position == null, "No position")
      Assertions.assertTrue(audioFormat.manifest == null, "No manifest")

      Assertions.assertEquals(
        formatHandle0,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/audiobook+json"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry0.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry0.book
    }

    val database1 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)
    val databaseEntry1 = database1.entry(bookID)

    val book1: org.nypl.simplified.books.api.Book = this.run {
      val formatHandle1 =
        databaseEntry1.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

      Assertions.assertTrue(formatHandle1 != null, "Format is present")

      this.checkOtherFormatsAreNotPresent(
        databaseEntry1, BookDatabaseEntryFormatHandleAudioBook::class.java
      )

      val audioFormat = databaseEntry1.book.findFormat(BookFormatAudioBook::class.java)
      Assertions.assertTrue(audioFormat != null, "Format is present")

      audioFormat!!
      Assertions.assertTrue(audioFormat.position == null, "No position")
      Assertions.assertTrue(audioFormat.manifest == null, "No manifest")

      Assertions.assertEquals(
        formatHandle1,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/audiobook+json"))
      )
      Assertions.assertEquals(
        null,
        databaseEntry1.findFormatHandleForContentType(mimeOf("application/not-a-supported-format"))
      )

      databaseEntry1.book
    }

    this.compareBooks(book0, book1)
  }

  /**
   * Creating a book database entry with an audio book format, and copying in a book and then
   * deleting the local book data repeatedly, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryAudioBookCopyDeleteRepeatedly() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithAudioBook()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    for (index in 0..2) {
      val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)
      Assertions.assertTrue(format != null, "Format is present")
      format!!

      val file = copyToTempFile("/org/nypl/simplified/tests/books/basic-manifest.json")
      format.copyInManifestAndURI(file.readBytes(), URI.create("urn:invalid"))
      format.deleteBookData()
    }

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)
    format!!
  }

  /**
   * Creating a book database entry with an audio book format, and copying in a book and then
   * destroying the entry, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryAudioBookCopyDestroyEntry() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithAudioBook()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)
    format!!
    val file = copyToTempFile("/org/nypl/simplified/tests/books/basic-manifest.json")
    format.copyInManifestAndURI(file.readBytes(), URI.create("urn:invalid"))

    databaseEntry.delete()
  }

  /**
   * Creating a book database entry with an epub format, and copying in a book and then
   * deleting the local book data repeatedly, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryEPUBCopyDeleteRepeatedly() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    for (index in 0..2) {
      val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
      Assertions.assertTrue(format != null, "Format is present")
      format!!

      val file = copyToTempFile("/org/nypl/simplified/tests/books/empty.epub")
      format.copyInBook(file)
      format.deleteBookData()
    }

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
    format!!
  }

  /**
   * Creating a book database entry with an epub format, and copying in a book and then
   * destroying the entry, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryEPUBCopyDestroyEntry() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
    format!!
    val file = copyToTempFile("/org/nypl/simplified/tests/books/empty.epub")
    format.copyInBook(file)

    databaseEntry.delete()
  }

  /**
   * Creating a book database entry with an pdf format, and copying in a book and then
   * deleting the local book data repeatedly, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryPDFCopyDeleteRepeatedly() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithPDF()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    for (index in 0..2) {
      val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)
      Assertions.assertTrue(format != null, "Format is present")
      format!!

      val file = copyToTempFile("/org/nypl/simplified/tests/books/empty.pdf")
      format.copyInBook(file)
      format.deleteBookData()
    }

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)
    format!!
  }

  /**
   * Creating a book database entry with an pdf format, and copying in a book and then
   * destroying the entry, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryPDFCopyDestroyEntry() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithPDF()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)
    format!!
    val file = copyToTempFile("/org/nypl/simplified/tests/books/empty.pdf")
    format.copyInBook(file)

    databaseEntry.delete()
  }

  /**
   * Creating a book database entry with an pdf format, and copying in a book and then
   * destroying the entry, works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryAudioBookSaveLoadClearPosition() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithAudioBook()
    val bookID = org.nypl.simplified.books.api.BookID.create("abcd")
    val databaseEntry = database0.createOrUpdate(bookID, feedEntry)

    val format = databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)
    format!!
    format.savePlayerPosition(
      PlayerPosition(title = "Title", part = 0, chapter = 1, offsetMilliseconds = 23L)
    )

    run {
      val book = databaseEntry.book
      val bookFormat = book.findFormat(BookFormatAudioBook::class.java)
      val position = bookFormat!!.position!!
      Assertions.assertEquals("Title", position.title)
      Assertions.assertEquals(0, position.part)
      Assertions.assertEquals(1, position.chapter)
      Assertions.assertEquals(23L, position.offsetMilliseconds)
    }

    format.savePlayerPosition(
      PlayerPosition(title = "Title 2", part = 2, chapter = 3, offsetMilliseconds = 46L)
    )

    run {
      val book = databaseEntry.book
      val bookFormat = book.findFormat(BookFormatAudioBook::class.java)
      val position = bookFormat!!.position!!
      Assertions.assertEquals("Title 2", position.title)
      Assertions.assertEquals(2, position.part)
      Assertions.assertEquals(3, position.chapter)
      Assertions.assertEquals(46L, position.offsetMilliseconds)
    }

    format.clearPlayerPosition()

    run {
      val book = databaseEntry.book
      val bookFormat = book.findFormat(BookFormatAudioBook::class.java)
      Assertions.assertEquals(null, bookFormat!!.position)
    }
  }

  private fun compareBooks(book0: org.nypl.simplified.books.api.Book, book1: org.nypl.simplified.books.api.Book) {
    Assertions.assertEquals(book0.account, book1.account)
    Assertions.assertEquals(book0.cover, book1.cover)
    Assertions.assertEquals(book0.formats, book1.formats)
    Assertions.assertEquals(book0.id, book1.id)
    Assertions.assertEquals(book0.thumbnail, book1.thumbnail)
  }

  private fun <T : org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle> checkOtherFormatsAreNotPresent(
    entry: org.nypl.simplified.books.book_database.api.BookDatabaseEntryType,
    clazz: Class<T>
  ) {
    val others =
      entry.formatHandles
        .filter { handle -> !clazz.isAssignableFrom(handle.javaClass) }

    this.logger.debug("other handles: {}", others)
    Assertions.assertEquals(0, others.size)
  }

  private fun acquisitionFeedEntryWithPDF(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke)
    )
    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        mimeOf("application/pdf"),
        emptyList()
      )
    )
    return eb.build()
  }

  private fun acquisitionFeedEntryWithAudioBook(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke)
    )
    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        mimeOf("application/audiobook+json"),
        emptyList()
      )
    )
    return eb.build()
  }

  private fun acquisitionFeedEntryWithEPUB(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke)
    )

    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        mimeOf("application/epub+zip"),
        emptyList()
      )
    )
    return eb.build()
  }

  @Throws(IOException::class)
  private fun copyToTempFile(
    name: String
  ): File {
    val file = File.createTempFile("simplified-book-database-", ".bin")
    logger.debug("copyToTempFile: {} -> {}", name, file)
    FileOutputStream(file).use { output ->
      BookDatabaseContract::class.java.getResourceAsStream(name)!!.use { input ->
        val buffer = ByteArray(4096)
        while (true) {
          val r = input.read(buffer)
          if (r == -1) {
            break
          }
          output.write(buffer, 0, r)
        }
        return file
      }
    }
  }

  private fun mimeOf(name: String): MIMEType {
    return MIMEParser.parseRaisingException(name)
  }
}
