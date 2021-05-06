package org.nypl.simplified.metrics

import android.content.Context
import org.nypl.simplified.metrics.api.MetricServiceFactoryType
import org.nypl.simplified.metrics.api.MetricServiceType

class MetricServiceFactory : MetricServiceFactoryType {
  override fun create(context: Context): MetricServiceType = MetricService(context)
}
