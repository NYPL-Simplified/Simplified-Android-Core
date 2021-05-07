package org.nypl.simplified.metrics.api

sealed class MetricEvent(val key: String) {
  data class LibraryAdded(val id: String) : MetricEvent("library_added")
  data class LibraryRemoved(val id: String) : MetricEvent("library_removed")
}
