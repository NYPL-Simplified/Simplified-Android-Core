package org.nypl.simplified.tests.books.book_database

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.books.book_database.BookDRMInformationHandleACS
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.tests.TestDirectories
import java.io.File

class BookDRMInformationHandleACSTest {

  private var updates: Int = 0
  private lateinit var directory1: File
  private lateinit var directory0: File

  @BeforeEach
  fun testSetup() {
    this.directory0 = TestDirectories.temporaryDirectory()
    this.directory1 = TestDirectories.temporaryDirectory()
    this.updates = 0
  }

  @AfterEach
  fun testTearDown() {
    DirectoryUtilities.directoryDelete(this.directory0)
    DirectoryUtilities.directoryDelete(this.directory1)
  }

  private fun countUpdateCalls() {
    this.updates += 1
  }

  /**
   * Creating a handle from an empty directory yields an empty handle.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEmpty() {
    val handle =
      BookDRMInformationHandleACS(
        directory = this.directory0,
        format = BOOK_FORMAT_EPUB,
        onUpdate = this::countUpdateCalls
      )
    assertEquals("ACS", File(this.directory0, "epub-drm.txt").readText())

    assertEquals(null, handle.info.acsmFile)
    assertEquals(null, handle.info.rights)
  }

  /**
   * Copying in an ACSM saves the ACSM.
   *
   * @throws Exception On errors
   */

  @Test
  fun testACSMCopyInEPUB() {
    val handle0 =
      BookDRMInformationHandleACS(
        directory = this.directory0,
        format = BOOK_FORMAT_EPUB,
        onUpdate = this::countUpdateCalls
      )
    assertEquals("ACS", File(this.directory0, "epub-drm.txt").readText())

    val acsm = this.resource("adobe-token.xml")

    val info0 = handle0.setACSMFile(acsm)
    assertEquals(info0, handle0.info)
    assertEquals("epub-meta_adobe.acsm", info0.acsmFile?.name)
    assertArrayEquals(acsm.readBytes(), info0.acsmFile?.readBytes())
    assertEquals(null, info0.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_EPUB,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info0, handle1.info)
    }

    val info1 = handle0.setACSMFile(null)
    assertEquals(info1, handle0.info)
    assertEquals(null, info1.acsmFile)
    assertEquals(null, info1.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_EPUB,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info1, handle1.info)
    }
  }

  /**
   * Copying in an ACSM saves the ACSM.
   *
   * @throws Exception On errors
   */

  @Test
  fun testACSMCopyInPDF() {
    val handle0 =
      BookDRMInformationHandleACS(
        directory = this.directory0,
        format = BOOK_FORMAT_PDF,
        onUpdate = this::countUpdateCalls
      )
    assertEquals("ACS", File(this.directory0, "pdf-drm.txt").readText())

    val acsm = this.resource("adobe-token.xml")

    val info0 = handle0.setACSMFile(acsm)
    assertEquals(info0, handle0.info)
    assertEquals("pdf-meta_adobe.acsm", info0.acsmFile?.name)
    assertArrayEquals(acsm.readBytes(), info0.acsmFile?.readBytes())
    assertEquals(null, info0.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_PDF,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info0, handle1.info)
    }

    val info1 = handle0.setACSMFile(null)
    assertEquals(info1, handle0.info)
    assertEquals(null, info1.acsmFile)
    assertEquals(null, info1.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_PDF,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info1, handle1.info)
    }
  }

  /**
   * Setting a loan saves the loan.
   *
   * @throws Exception On errors
   */

  @Test
  fun testLoanSetEPUB() {
    val handle0 =
      BookDRMInformationHandleACS(
        directory = this.directory0,
        format = BOOK_FORMAT_EPUB,
        onUpdate = this::countUpdateCalls
      )
    assertEquals("ACS", File(this.directory0, "epub-drm.txt").readText())

    val loan =
      AdobeAdeptLoan(
        AdobeLoanID("1e2869c2-1fd3-47d2-a5ac-a4e24093a64a"),
        ByteArray(23),
        true
      )

    val info0 = handle0.setAdobeRightsInformation(loan)
    assertEquals(info0, handle0.info)
    assertEquals(null, info0.acsmFile)
    assertEquals(Pair(File(directory0, "epub-rights_adobe.xml"), loan), info0.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_EPUB,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info0, handle1.info)
    }

    val info1 = handle0.setAdobeRightsInformation(null)
    assertEquals(info1, handle0.info)
    assertEquals(null, info1.acsmFile)
    assertEquals(null, info1.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_EPUB,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info1, handle1.info)
    }
  }

  /**
   * Setting a loan saves the loan.
   *
   * @throws Exception On errors
   */

  @Test
  fun testLoanSetPDF() {
    val handle0 =
      BookDRMInformationHandleACS(
        directory = this.directory0,
        format = BOOK_FORMAT_PDF,
        onUpdate = this::countUpdateCalls
      )
    assertEquals("ACS", File(this.directory0, "pdf-drm.txt").readText())

    val loan =
      AdobeAdeptLoan(
        AdobeLoanID("1e2869c2-1fd3-47d2-a5ac-a4e24093a64a"),
        ByteArray(233),
        true
      )

    val info0 = handle0.setAdobeRightsInformation(loan)
    assertEquals(info0, handle0.info)
    assertEquals(null, info0.acsmFile)
    assertEquals(Pair(File(directory0, "pdf-rights_adobe.xml"), loan), info0.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_PDF,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info0, handle1.info)
    }

    val info1 = handle0.setAdobeRightsInformation(null)
    assertEquals(info1, handle0.info)
    assertEquals(null, info1.acsmFile)
    assertEquals(null, info1.rights)

    run {
      val handle1 =
        BookDRMInformationHandleACS(
          directory = this.directory0,
          format = BOOK_FORMAT_PDF,
          onUpdate = this::countUpdateCalls
        )
      assertEquals(info1, handle1.info)
    }
  }

  private fun resource(
    name: String
  ): File {
    return BookDRMInformationHandleACSTest::class.java.getResourceAsStream(
      "/org/nypl/simplified/tests/books/$name"
    ).use { stream ->
      val out = File(this.directory1, name)
      FileUtilities.fileWriteStream(out, stream)
      out
    }
  }
}
