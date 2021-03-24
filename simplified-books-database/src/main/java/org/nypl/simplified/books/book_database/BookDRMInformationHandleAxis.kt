package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.files.FileUtilities
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An information handle for AxisNow.
 */

class BookDRMInformationHandleAxis(
  private val directory: File,
  format: BookFormats.BookFormatDefinition,
  private val onUpdate: () -> Unit
) : BookDRMInformationHandle.AxisHandle(), BookDRMInformationHandleBase {

  private val closed = AtomicBoolean(false)

  companion object {
    fun nameLicense(format: BookFormats.BookFormatDefinition) =
      "${format.shortName}-license_axis.json"

    fun nameUserKey(format: BookFormats.BookFormatDefinition) =
      "${format.shortName}-key_axis"
  }

  init {
    BookDRMInformationHandles.writeDRMInfo(
      directory = directory,
      format = format,
      kind = BookDRMKind.AXIS
    )
  }

  private val fileLicense =
    File(this.directory, nameLicense(format))
  private val fileUserKey =
    File(this.directory, nameUserKey(format))

  private val infoLock: Any = Any()
  private var infoRef: BookDRMInformation.AXIS =
    synchronized(this.infoLock) {
      this.loadInitial(fileLicense, fileUserKey)
    }

  private fun loadInitial(
    license: File,
    userKey: File
  ): BookDRMInformation.AXIS {
    return BookDRMInformation.AXIS(
      license = license.takeIf { it.isFile },
      userKey = userKey.takeIf { it.isFile }
    )
  }

  override val info: BookDRMInformation.AXIS
    get() {
      check(!this.closed.get()) { "Handle must not have been closed" }
      return synchronized(this.infoLock) { this.infoRef }
    }

  override fun copyInAxisLicense(file: File): BookDRMInformation.AXIS {
    synchronized(this.infoLock) {
      FileUtilities.fileCopy(file, this.fileLicense)
      this.infoRef = this.infoRef.copy(license = this.fileLicense)
    }

    this.onUpdate.invoke()
    return this.infoRef
  }

  override fun copyInAxisUserKey(file: File): BookDRMInformation.AXIS {
    synchronized(this.infoLock) {
      FileUtilities.fileCopy(file, this.fileUserKey)
      this.infoRef = this.infoRef.copy(userKey = fileUserKey)
    }

    this.onUpdate.invoke()
    return this.infoRef
  }

  override fun close() {
    this.closed.compareAndSet(false, true)
  }
}
