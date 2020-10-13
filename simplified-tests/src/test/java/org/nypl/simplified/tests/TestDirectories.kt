package org.nypl.simplified.tests

import java.io.File
import java.io.IOException
import java.util.UUID

object TestDirectories {

  @Throws(IOException::class)
  fun temporaryFileOf(
    name: String,
    data: String
  ): File {
    val file = File(temporaryDirectory(), name)
    file.writeText(data)
    return file
  }

  @Throws(IOException::class)
  fun temporaryDirectory(): File {
    val dir = temporaryBaseDirectory()
    val temp = File(dir, UUID.randomUUID().toString())
    temp.mkdirs()
    return temp
  }

  @Throws(IOException::class)
  fun temporaryBaseDirectory(): File {
    val tmpBase = File(System.getProperty("java.io.tmpdir"))
    val path1 = File(tmpBase, "org.nypl.simplified")
    path1.mkdirs()
    return path1
  }
}
