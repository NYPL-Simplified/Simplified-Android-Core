package org.nypl.simplified.ui.splash

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.migration.spi.MigrationEvent

/**
 * A recycler view adapter for migration reports.
 */

class MigrationReportListAdapter(
  private val events: List<MigrationEvent>
) :
  RecyclerView.Adapter<MigrationReportListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.splash_migration_report_item, parent, false)

    return this.ViewHolder(item)
  }

  override fun getItemCount(): Int =
    this.events.size

  private val matrixOK =
    SplashColorMatrix.getImageFilterMatrix(Color.BLACK)
  private val matrixError =
    SplashColorMatrix.getImageFilterMatrix(Color.RED)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val event = this.events[position]

    when (event) {
      is MigrationEvent.MigrationStepInProgress -> {
        // Ignored, not present!
      }

      is MigrationEvent.MigrationStepSucceeded -> {
        when (event.subject) {
          MigrationEvent.Subject.ACCOUNT -> {
            holder.icon.visibility = View.VISIBLE
            holder.icon.colorFilter = this.matrixOK
            holder.icon.setImageResource(R.drawable.migration_account)
          }
          MigrationEvent.Subject.BOOK -> {
            holder.icon.visibility = View.VISIBLE
            holder.icon.colorFilter = this.matrixOK
            holder.icon.setImageResource(R.drawable.migration_book)
          }
          MigrationEvent.Subject.BOOKMARK -> {
            holder.icon.visibility = View.VISIBLE
            holder.icon.colorFilter = this.matrixOK
            holder.icon.setImageResource(R.drawable.migration_bookmark)
          }
          MigrationEvent.Subject.PROFILE,
          null -> {
            holder.icon.visibility = View.INVISIBLE
          }
        }

        holder.description.setTextColor(Color.BLACK)
        holder.description.text = event.message
      }

      is MigrationEvent.MigrationStepError -> {
        holder.icon.visibility = View.VISIBLE
        holder.icon.colorFilter = this.matrixError
        holder.icon.setImageResource(R.drawable.migration_error)
        holder.description.setTextColor(Color.RED)
        holder.description.text = event.message
      }
    }
  }

  inner class ViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
    val icon =
      parent.findViewById<ImageView>(R.id.splashMigrationReportItemIcon)
    val description =
      parent.findViewById<TextView>(R.id.splashMigrationReportItemText)
  }
}
