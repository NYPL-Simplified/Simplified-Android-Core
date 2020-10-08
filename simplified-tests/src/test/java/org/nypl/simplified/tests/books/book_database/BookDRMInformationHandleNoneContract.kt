package org.nypl.simplified.tests.books.book_database

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nypl.simplified.books.book_database.BookDRMInformationHandleNone
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.tests.TestDirectories
import java.io.File

abstract class BookDRMInformationHandleNoneContract {

  private lateinit var directory1: File
  private lateinit var directory0: File

  @Before
  fun testSetup() {
    this.directory0 = TestDirectories.temporaryDirectory()
    this.directory1 = TestDirectories.temporaryDirectory()
  }

  @After
  fun testTearDown() {
    DirectoryUtilities.directoryDelete(this.directory0)
    DirectoryUtilities.directoryDelete(this.directory1)
  }

  /**
   * Creating a handle from an empty directory yields an empty handle.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEmptyEPUB() {
    val handle =
      BookDRMInformationHandleNone(
        directory = this.directory0,
        format = BOOK_FORMAT_EPUB
      )
    assertEquals("NONE", File(this.directory0, "epub-drm.txt").readText())
  }

  /**
   * Creating a handle from an empty directory yields an empty handle.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEmptyPDF() {
    val handle =
      BookDRMInformationHandleNone(
        directory = this.directory0,
        format = BOOK_FORMAT_PDF
      )
    assertEquals("NONE", File(this.directory0, "pdf-drm.txt").readText())
  }
}
