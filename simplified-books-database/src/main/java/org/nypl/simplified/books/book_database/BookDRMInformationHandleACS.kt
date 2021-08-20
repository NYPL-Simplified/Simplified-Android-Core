package org.nypl.simplified.books.book_database

import com.fasterxml.jackson.databind.ObjectMapper
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An information handle for Adobe ACS DRM.
 */

class BookDRMInformationHandleACS(
  private val directory: File,
  format: BookFormats.BookFormatDefinition,
  private val onUpdate: () -> Unit
) : BookDRMInformationHandle.ACSHandle(), BookDRMInformationHandleBase {

  private val objectMapper = ObjectMapper()
  private val closed = AtomicBoolean(false)

  companion object {
    fun nameRights(format: BookFormats.BookFormatDefinition) =
      "${format.shortName}-rights_adobe.xml"

    fun nameACSM(format: BookFormats.BookFormatDefinition) =
      "${format.shortName}-meta_adobe.acsm"

    fun nameMetaJSON(format: BookFormats.BookFormatDefinition) =
      "${format.shortName}-meta_adobe.json"

    fun names(format: BookFormats.BookFormatDefinition) =
      listOf(
        nameRights(format),
        nameACSM(format),
        nameMetaJSON(format)
      )

    fun deleteFiles(directory: File) {
    }
  }

  init {
    BookDRMInformationHandles.writeDRMInfo(
      directory = this.directory,
      format = format,
      kind = BookDRMKind.ACS
    )
  }

  private val fileAdobeRights =
    File(this.directory, nameRights(format))
  private val fileAdobeRightsTmp =
    File(this.directory, nameRights(format) + ".tmp")
  private val fileAdobeACSM =
    File(this.directory, nameACSM(format))
  private val fileAdobeACSMTmp =
    File(this.directory, nameACSM(format) + ".tmp")
  private val fileAdobeMeta =
    File(this.directory, nameMetaJSON(format))
  private val fileAdobeMetaTmp =
    File(this.directory, nameMetaJSON(format) + ".tmp")

  private val infoLock: Any = Any()
  private var infoRef: BookDRMInformation.ACS =
    synchronized(this.infoLock) {
      this.loadInitial(
        fileAdobeRights = this.fileAdobeRights,
        fileAdobeACSM = this.fileAdobeACSM,
        fileAdobeMeta = this.fileAdobeMeta
      )
    }

  private fun loadInitial(
    fileAdobeRights: File,
    fileAdobeACSM: File,
    fileAdobeMeta: File
  ): BookDRMInformation.ACS {
    return BookDRMInformation.ACS(
      acsmFile = if (fileAdobeACSM.isFile) fileAdobeACSM else null,
      rights = this.loadAdobeRightsInformationIfPresent(fileAdobeRights, fileAdobeMeta)
    )
  }

  @Throws(IOException::class)
  private fun loadAdobeRightsInformationIfPresent(
    fileAdobeRights: File,
    fileAdobeMeta: File
  ): Pair<File, AdobeAdeptLoan>? {
    return if (fileAdobeRights.isFile) {
      this.loadAdobeRightsInformation(fileAdobeRights, fileAdobeMeta)
    } else {
      null
    }
  }

  @Throws(IOException::class)
  private fun loadAdobeRightsInformation(
    fileAdobeRights: File,
    fileAdobeMeta: File
  ): Pair<File, AdobeAdeptLoan>? {
    val serialized = fileAdobeRights.readBytes()
    val jn = this.objectMapper.readTree(fileAdobeMeta)
    val o = JSONParserUtilities.checkObject(null, jn)
    val loanID = AdobeLoanID(JSONParserUtilities.getString(o, "loan-id"))
    val returnable = JSONParserUtilities.getBoolean(o, "returnable")
    val loan = AdobeAdeptLoan(loanID, serialized, returnable)
    return Pair(fileAdobeRights, loan)
  }

  override val info: BookDRMInformation.ACS
    get() {
      check(!this.closed.get()) { "Handle must not have been closed" }
      return synchronized(this.infoLock) { this.infoRef }
    }

  @Throws(IOException::class)
  override fun setACSMFile(
    acsm: File?
  ): BookDRMInformation.ACS {
    check(!this.closed.get()) { "Handle must not have been closed" }

    return try {
      synchronized(this.infoLock) {
        if (acsm != null) {
          FileUtilities.fileWriteBytesAtomically(
            this.fileAdobeACSM,
            this.fileAdobeACSMTmp,
            FileUtilities.fileReadBytes(acsm)
          )
          this.infoRef = this.infoRef.copy(acsmFile = this.fileAdobeACSM)
          this.infoRef
        } else {
          FileUtilities.fileDelete(this.fileAdobeACSM)
          FileUtilities.fileDelete(this.fileAdobeACSMTmp)
          this.infoRef = this.infoRef.copy(acsmFile = null)
          this.infoRef
        }
      }
    } finally {
      this.onUpdate.invoke()
    }
  }

  @Throws(IOException::class)
  override fun setAdobeRightsInformation(
    loan: AdobeAdeptLoan?
  ): BookDRMInformation.ACS {
    check(!this.closed.get()) { "Handle must not have been closed" }

    return try {
      synchronized(this.infoLock) {
        if (loan != null) {
          FileUtilities.fileWriteBytesAtomically(
            this.fileAdobeRights,
            this.fileAdobeRightsTmp,
            loan.serialized
          )

          val o = this.objectMapper.createObjectNode()
          o.put("loan-id", loan.id.value)
          o.put("returnable", loan.isReturnable)

          ByteArrayOutputStream().use { stream ->
            JSONSerializerUtilities.serialize(o, stream)
            FileUtilities.fileWriteUTF8Atomically(
              this.fileAdobeMeta,
              this.fileAdobeMetaTmp,
              stream.toString("UTF-8")
            )
          }

          this.infoRef = this.infoRef.copy(rights = Pair(this.fileAdobeRights, loan))
          this.infoRef
        } else {
          FileUtilities.fileDelete(this.fileAdobeMeta)
          FileUtilities.fileDelete(this.fileAdobeMetaTmp)
          FileUtilities.fileDelete(this.fileAdobeRights)
          FileUtilities.fileDelete(this.fileAdobeRightsTmp)

          this.infoRef = this.infoRef.copy(rights = null)
          this.infoRef
        }
      }
    } finally {
      this.onUpdate.invoke()
    }
  }

  override fun close() {
    this.closed.compareAndSet(false, true)
  }
}
