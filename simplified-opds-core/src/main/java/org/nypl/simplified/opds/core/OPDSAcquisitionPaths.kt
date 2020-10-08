package org.nypl.simplified.opds.core

/**
 * Functions to transform lists of acquisitions into lists of acquisition paths.
 *
 * @see "https://github.com/io7m/opds-acquisition-spec"
 */

object OPDSAcquisitionPaths {

  private fun indirectPathsInner(
    elementStack: MutableList<OPDSAcquisitionPathElement>,
    output: MutableList<OPDSAcquisitionPath>,
    source: OPDSAcquisition,
    indirect: OPDSIndirectAcquisition
  ) {
    val newPathElement = OPDSAcquisitionPathElement(indirect.type, null)
    if (indirect.indirectAcquisitions.isEmpty()) {
      val pathElements = mutableListOf<OPDSAcquisitionPathElement>()
      pathElements.addAll(elementStack)
      pathElements.add(newPathElement)
      output.add(OPDSAcquisitionPath(source, pathElements.toList()))
    } else {
      elementStack.add(newPathElement)
      for (subIndirect in indirect.indirectAcquisitions) {
        this.indirectPathsInner(elementStack, output, source, subIndirect)
      }
      elementStack.removeAt(elementStack.size - 1)
    }
  }

  private fun indirectPaths(
    source: OPDSAcquisition,
    indirect: OPDSIndirectAcquisition
  ): List<OPDSAcquisitionPath> {
    val paths = mutableListOf<OPDSAcquisitionPath>()
    this.indirectPathsInner(mutableListOf(), paths, source, indirect)
    return paths.toList()
  }

  private fun acquisitionPaths(
    source: OPDSAcquisition
  ): List<OPDSAcquisitionPath> {
    val paths = mutableListOf<OPDSAcquisitionPath>()
    if (source.indirectAcquisitions.isEmpty()) {
      paths.add(OPDSAcquisitionPath(source, listOf(OPDSAcquisitionPathElement(source.type, source.uri))))
    } else {
      for (indirect in source.indirectAcquisitions) {
        paths.addAll(
          this.indirectPaths(source, indirect)
            .map { path -> path.prefixWith(source.type, source.uri) }
        )
      }
    }
    return paths.toList()
  }

  /**
   * Transform the given list of acquisitions into a list of acquisition paths.
   */

  fun linearize(
    acquisitions: List<OPDSAcquisition>
  ): List<OPDSAcquisitionPath> {
    val paths = mutableListOf<OPDSAcquisitionPath>()
    for (acquisition in acquisitions) {
      paths.addAll(this.linearize(acquisition))
    }
    return paths.toList()
  }

  /**
   * Transform the given acquisition into a list of acquisition paths.
   */

  fun linearize(
    acquisition: OPDSAcquisition
  ): List<OPDSAcquisitionPath> {
    return this.acquisitionPaths(acquisition)
  }

  /**
   * Transform the given feed entry into a list of acquisition paths.
   */

  fun linearize(
    entry: OPDSAcquisitionFeedEntry
  ): List<OPDSAcquisitionPath> {
    return this.linearize(entry.acquisitions)
  }
}
