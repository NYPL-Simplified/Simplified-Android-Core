package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import com.google.common.base.Preconditions
import org.nypl.simplified.books.feeds.FeedFacetType
import java.util.ArrayList
import java.util.Objects

/**
 * A button that shows a list of facets.
 */

class CatalogFacetButton(
  val activity: AppCompatActivity,
  private val groupName: String,
  val group: ArrayList<FeedFacetType>,
  val listener: CatalogFacetSelectionListenerType) 
  : AppCompatButton(activity) {

  init {
    Preconditions.checkArgument(
      !group.isEmpty(), "Facet group is not empty")

    var active_maybe = Objects.requireNonNull(group[0])
    for (f in group) {
      if (f.facetIsActive()) {
        active_maybe = Objects.requireNonNull(f)
        break
      }
    }

    val active = Objects.requireNonNull(active_maybe)
    this.text = active.facetGetTitle()
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
