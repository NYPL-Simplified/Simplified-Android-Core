package org.nypl.simplified.ui.splash

import android.content.Context
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.reports.Reports
import java.util.ServiceLoader

internal data class MigrationReportEmail(
  val subject: String,
  val body: String,
  val email: String
) {

  fun send(context: Context) {
    Reports.sendReportsDefault(
      context = context,
      address = email,
      subject = subject,
      body = body
    )
  }

  companion object {

    fun fromMigrationReport(report: MigrationReport): MigrationReportEmail {
      return MigrationReportEmail(
        subject = reportEmailSubject(report),
        body = reportEmailBody(report),
        email = getBuildConfigService().supportErrorReportEmailAddress
      )
    }

    private fun reportEmailBody(report: MigrationReport): String {
      val errors = report.events.filterIsInstance<MigrationEvent.MigrationStepError>().size

      return StringBuilder(128)
        .append("On ${report.timestamp}, a migration of ${report.application} occurred.")
        .append("\n")
        .append("There were $errors errors.")
        .append("\n")
        .append("The attached log files give details of the migration.")
        .append("\n")
        .toString()
    }

    private fun reportEmailSubject(report: MigrationReport): String {
      val errors =
        report.events.any { e -> e is MigrationEvent.MigrationStepError }
      val outcome =
        if (errors) {
          "error"
        } else {
          "success"
        }

      return "[simplye-android-migration] ${report.application} $outcome"
    }

    private fun getBuildConfigService(): BuildConfigurationServiceType {
      return ServiceLoader
        .load(BuildConfigurationServiceType::class.java)
        .firstOrNull()
        ?: throw IllegalStateException(
          "No available services of type ${BuildConfigurationServiceType::class.java.canonicalName}"
        )
    }
  }
}
