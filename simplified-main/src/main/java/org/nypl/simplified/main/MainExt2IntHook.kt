package org.nypl.simplified.main

import android.content.Context
import android.os.Environment
import com.google.common.base.Preconditions
import org.nypl.simplified.boot.api.BootPreHookType
import org.nypl.simplified.main.MainServices.CURRENT_DATA_VERSION
import org.slf4j.LoggerFactory
import java.io.File

/**
 * A pre-boot hook to migrate data from the external storage to internal storage. Applications
 * prior to the mid-2020 5.* branch may have been using external storage. We now require internal
 * storage to be used at all times.
 */

class MainExt2IntHook : BootPreHookType {
  private val logger = LoggerFactory.getLogger(MainExt2IntHook::class.java)

  private fun findOldExternalDirectory(context: Context): File? {
    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      this.logger.debug("trying external storage")
      if (!Environment.isExternalStorageRemovable()) {
        val result = context.getExternalFilesDir(null)
        this.logger.debug("external storage is not removable, using it ({})", result)
        Preconditions.checkArgument(result!!.isDirectory, "Data directory {} is a directory", result)
        return File(result, CURRENT_DATA_VERSION)
      }
    }

    this.logger.debug("external storage is unsuitable, ignoring it")
    return null
  }

  override fun execute(context: Context) {
    this.logger.debug("executing")

    val newDirectory = File(context.filesDir, CURRENT_DATA_VERSION)
    val oldDirectory = this.findOldExternalDirectory(context)
    if (oldDirectory != null) {
      oldDirectory.delete()

      if (oldDirectory.isDirectory) {
        newDirectory.mkdirs()
        this.logger.debug("copying old external {} to internal {}", oldDirectory, newDirectory)
        oldDirectory.copyRecursively(
          target = newDirectory,
          overwrite = false,
          onError = { file, exception ->
            this.logger.error("failed to copy file: {}: ", file, exception)
            OnErrorAction.SKIP
          }
        )
        oldDirectory.deleteRecursively()
        this.logger.debug("deleted old directory")
      }
    } else {
      this.logger.debug("nothing to do!")
    }
  }
}
