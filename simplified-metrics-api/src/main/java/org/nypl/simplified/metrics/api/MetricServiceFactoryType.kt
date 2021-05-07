package org.nypl.simplified.metrics.api

import android.content.Context

interface MetricServiceFactoryType {
  fun create(context: Context): MetricServiceType
}
