package org.nypl.simplified.app.catalog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.common.base.Preconditions
import org.nypl.simplified.feeds.api.FeedFacet
import java.util.ArrayList
import java.util.Objects

/**
 * A button that shows a list of facets.
 */

class CatalogFacetButton(
  val activity: AppCompatActivity,
  private val groupName: String,
  val group: ArrayList<FeedFacet>,
  val listener: CatalogFacetSelectionListenerType
) : AppCompatButton(activity) {

  init {
    Preconditions.checkArgument(
      !group.isEmpty(), "Facet group is not empty")

    var active_maybe = Objects.requireNonNull(group[0])
    for (f in group) {
      if (f.isActive) {
        active_maybe = Objects.requireNonNull(f)
        break
      }
    }

    val active = Objects.requireNonNull(active_maybe)
    this.text = active.title
    this.setOnClickListener { view ->
      val fm = activity.fragmentManager
      val d = CatalogFacetDialog.newDialog(groupName, group)
      d.setFacetSelectionListener { facet ->
        d.dismiss()
        listener.onFacetSelected(facet)
      }
      d.show(fm, "facet-dialog")
    }
  }
}
