package org.nypl.simplified.migration.api

import org.nypl.simplified.migration.spi.MigrationReport
import org.w3c.dom.Document
import java.io.OutputStream

/**
 * Functions to serialize reports to XML.
 */

interface MigrationReportXMLType {

  /**
   * Serialize the given report to an XML document.
   */

  fun serializeToXML(
    report: MigrationReport
  ): Document

  /**
   * Serialize the given report to XML on the given output stream.
   */

  fun serializeToXML(
    report: MigrationReport,
    outputStream: OutputStream)

}
