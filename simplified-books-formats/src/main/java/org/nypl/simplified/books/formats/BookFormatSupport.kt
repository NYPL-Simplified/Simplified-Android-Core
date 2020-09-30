package org.nypl.simplified.books.formats

import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.slf4j.LoggerFactory

/**
 * The main implementation of the [BookFormatSupportType] API.
 */

class BookFormatSupport private constructor(
  private val parameters: BookFormatSupportParameters
) : BookFormatSupportType {

  private val logger =
    LoggerFactory.getLogger(BookFormatSupport::class.java)

  private val finalTypes: Set<MIMEType>
  private val knownTypes: Set<MIMEType>

  init {
    this.finalTypes = this.collectFinalTypes()
    this.knownTypes = this.collectKnownTypes()
  }

  private fun collectKnownTypes(): Set<MIMEType> {
    val types = mutableSetOf<MIMEType>()
    types.add(StandardFormatNames.genericEPUBFiles)
    types.add(StandardFormatNames.simplifiedBearerToken)
    types.addAll(StandardFormatNames.allOPDSFeeds)

    this.collectSupportedAudioBookTypesInto(types)

    if (this.parameters.supportsAdobeDRM) {
      types.add(StandardFormatNames.adobeACSMFiles)
    }
    if (this.parameters.supportsPDF) {
      types.add(StandardFormatNames.genericPDFFiles)
    }
    return types.toSet()
  }

  private fun collectFinalTypes(): Set<MIMEType> {
    val types = mutableSetOf<MIMEType>()
    types.add(StandardFormatNames.genericEPUBFiles)
    this.collectSupportedAudioBookTypesInto(types)

    if (this.parameters.supportsPDF) {
      types.add(StandardFormatNames.genericPDFFiles)
    }
    return types.toSet()
  }

  private fun collectSupportedAudioBookTypesInto(types: MutableSet<MIMEType>) {
    val audio = this.parameters.supportsAudioBooks
    if (audio != null) {
      types.addAll(StandardFormatNames.genericAudioBooks)
      if (audio.supportsFindawayAudioBooks) {
        types.add(StandardFormatNames.findawayAudioBooks)
      }
      if (audio.supportsOverdriveAudioBooks) {
        types.add(StandardFormatNames.overdriveAudioBooks)
      }
    }
  }

  companion object {

    /**
     * Construct a new book format support API.
     */

    fun create(
      parameters: BookFormatSupportParameters
    ): BookFormatSupportType {
      return BookFormatSupport(parameters)
    }
  }

  override fun isSupportedFinalContentType(
    mime: MIMEType
  ): Boolean {
    return this.finalTypes.contains(mime)
  }

  override fun isSupportedPath(
    typePath: List<MIMEType>
  ): Boolean {
    /*
     * An empty path is trivially unsupported.
     */

    if (typePath.isEmpty()) {
      return false
    }

    /*
     * Check that all of the types in the path are known.
     */

    for (requestedType in typePath) {
      if (!this.knownTypes.contains(requestedType)) {
        this.logger.warn("MIME type {} is not supported", requestedType)
        return false
      }
    }

    /*
     * Check that the final element of the path is an accepted final type.
     */

    val typeLast = typePath.last()
    if (!this.isSupportedFinalContentType(typeLast)) {
      this.logger.warn("MIME type {} is not a supported final type", typeLast)
      return false
    }

    /*
     * Check that the path doesn't imply an Adobe-encrypted PDF.
     */

    val typeSet = typePath.toSet()
    if (typeSet.contains(StandardFormatNames.genericPDFFiles) &&
      typeSet.contains(StandardFormatNames.adobeACSMFiles)
    ) {
      this.logger.warn("Adobe-encrypted PDFs are not supported")
      return false
    }

    return true
  }
}
