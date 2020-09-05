package org.nypl.simplified.tests.opds

import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser.Companion.parseRaisingException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.opds.core.OPDSAcquisitionPaths
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition
import java.net.URI

abstract class OPDSAcquisitionPathsContract {

  private fun mimeOf(text: String): MIMEType {
    return try {
      parseRaisingException(text)
    } catch (e: Exception) {
      throw IllegalStateException(e)
    }
  }

  private fun pathElementOf(
    mime: String,
    uri: String? = null
  ): OPDSAcquisitionPathElement {
    return OPDSAcquisitionPathElement(this.mimeOf(mime), uri?.let { URI.create(it) })
  }

  @Test
  fun testEmpty() {
    assertEquals(listOf<OPDSAcquisitionPath>(), OPDSAcquisitionPaths.linearize(listOf()))
  }

  @Test
  fun testAcquisitionDirect() {
    val acquisition =
      OPDSAcquisition(
        relation = OPDSAcquisition.Relation.ACQUISITION_GENERIC,
        uri = URI.create("http://www.example.com"),
        type = this.mimeOf("application/epub+zip"),
        indirectAcquisitions = listOf()
      )

    val element0 =
      this.pathElementOf("application/epub+zip", "http://www.example.com")
    val path =
      OPDSAcquisitionPath(acquisition, listOf(element0))

    val linearized =
      OPDSAcquisitionPaths.linearize(acquisition)

    assertEquals(path, linearized[0])
    assertEquals(1, linearized.size)
  }

  @Test
  fun testAcquisitionAdobeIndirect() {
    val acquisition =
      OPDSAcquisition(
        relation = OPDSAcquisition.Relation.ACQUISITION_GENERIC,
        uri = URI.create("http://www.example.com"),
        type = this.mimeOf("application/vnd.adobe.adept+xml"),
        indirectAcquisitions = listOf(
          OPDSIndirectAcquisition(this.mimeOf("application/epub+zip"), listOf()),
          OPDSIndirectAcquisition(this.mimeOf("application/pdf"), listOf())
        )
      )

    val element0 =
      this.pathElementOf("application/vnd.adobe.adept+xml", "http://www.example.com")
    val element01 =
      this.pathElementOf("application/epub+zip")
    val element02 =
      this.pathElementOf("application/pdf")

    val path0 =
      OPDSAcquisitionPath(acquisition, listOf(element0, element01))
    val path1 =
      OPDSAcquisitionPath(acquisition, listOf(element0, element02))

    val linearized =
      OPDSAcquisitionPaths.linearize(acquisition)

    assertEquals(path0, linearized[0])
    assertEquals(path1, linearized[1])
    assertEquals(2, linearized.size)
  }
}
