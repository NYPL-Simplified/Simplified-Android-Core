package org.nypl.simplified.migration.api

import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.presentableerror.api.PresentableType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Functions to serialize reports to XML.
 */

object MigrationReportXML : MigrationReportXMLType {

  /**
   * Serialize the given report to an XML document.
   */

  override fun serializeToXML(
    report: MigrationReport
  ): Document {
    val documentBuilders = DocumentBuilderFactory.newInstance()
    val documentBuilder = documentBuilders.newDocumentBuilder()
    val document = documentBuilder.newDocument()

    val root = document.createElement("report")
    root.setAttribute("time", "${report.timestamp}")
    root.setAttribute("application", report.application)
    root.setAttribute("migration", report.migrationService)
    document.appendChild(root)

    val events = document.createElement("events")
    root.appendChild(events)

    var index = 0
    for (event in report.events) {
      val eventElement = document.createElement("event")
      eventElement.setAttribute("index", "$index")
      events.appendChild(eventElement)

      when (event) {
        is MigrationEvent.MigrationStepInProgress -> {
          eventElement.setAttribute("type", "MigrationStepInProgress")
          eventElement.setAttribute("message", event.message)
          eventElement.appendChild(this.saveAttributes(document, event.attributes))
        }

        is MigrationEvent.MigrationStepSucceeded -> {
          eventElement.setAttribute("type", "MigrationStepSucceeded")
          eventElement.setAttribute("message", event.message)
          eventElement.appendChild(this.saveAttributes(document, event.attributes))

          for (cause in event.causes) {
            eventElement.appendChild(this.saveCauseTree(document, cause))
          }
        }

        is MigrationEvent.MigrationStepError -> {
          eventElement.setAttribute("type", "MigrationStepError")
          eventElement.setAttribute("message", event.message)
          eventElement.appendChild(this.saveAttributes(document, event.attributes))

          val exception = event.exception
          if (exception != null) {
            eventElement.appendChild(this.saveException(document, exception))
          }
        }
      }
      ++index
    }

    return document
  }

  /**
   * Serialize the given report to XML on the given output stream.
   */

  override fun serializeToXML(
    report: MigrationReport,
    outputStream: OutputStream
  ) {
    val xmlSource = DOMSource(serializeToXML(report))
    val outputTarget = StreamResult(outputStream)
    TransformerFactory.newInstance()
      .newTransformer()
      .transform(xmlSource, outputTarget)
    outputStream.flush()
  }

  private fun saveCauseTree(
    document: Document,
    cause: PresentableType
  ): Element {
    val element = document.createElement("cause")
    element.setAttribute("message", cause.message)
    element.appendChild(this.saveAttributes(document, cause.attributes))
    return element
  }

  private fun saveException(
    document: Document,
    exception: Throwable
  ): Element {
    val exceptionElement = document.createElement("exception")
    ByteArrayOutputStream().use { stream ->
      PrintWriter(stream).use { writer -> exception.printStackTrace(writer) }
      exceptionElement.textContent = stream.toString("UTF-8")
    }
    return exceptionElement
  }

  private fun saveAttributes(
    document: Document,
    attributes: Map<String, String>
  ): Element {
    val attributesElement = document.createElement("attributes")
    for (key in attributes.keys) {
      val attributeElement = document.createElement("attribute")
      attributeElement.setAttribute("name", key)
      attributeElement.setAttribute("value", attributes[key] ?: "")
    }
    return attributesElement
  }
}
