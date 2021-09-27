package org.nypl.simplified.metrics.api

interface MetricServiceType {
  fun logMetric(event: MetricEvent)
}
