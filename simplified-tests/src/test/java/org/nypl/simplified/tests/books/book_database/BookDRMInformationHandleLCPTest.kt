package org.nypl.simplified.tests.books.book_database

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.books.book_database.BookDRMInformationHandleLCP
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.tests.TestDirectories
import java.io.File

class BookDRMInformationHandleLCPTest {

  private lateinit var directory1: File
  private lateinit var directory0: File

  @BeforeEach
  fun testSetup() {
    this.directory0 = TestDirectories.temporaryDirectory()
    this.directory1 = TestDirectories.temporaryDirectory()
  }

  @AfterEach
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
      BookDRMInformationHandleLCP(
        directory = this.directory0,
        format = BOOK_FORMAT_EPUB
      )
    assertEquals("LCP", File(this.directory0, "epub-drm.txt").readText())
  }

  /**
   * Creating a handle from an empty directory yields an empty handle.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEmptyPDF() {
    val handle =
      BookDRMInformationHandleLCP(
        directory = this.directory0,
        format = BOOK_FORMAT_PDF
      )
    assertEquals("LCP", File(this.directory0, "pdf-drm.txt").readText())
  }
}
