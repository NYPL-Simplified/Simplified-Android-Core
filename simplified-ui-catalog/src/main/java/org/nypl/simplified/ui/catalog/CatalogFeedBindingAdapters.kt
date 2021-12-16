package org.nypl.simplified.ui.catalog

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("showWhenFeedAgeGate")
fun View.showWhenFeedAgeGate(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedAgeGate)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedLoading")
fun View.showWhenFeedLoading(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoading)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedWithGroups")
fun View.showWhenFeedWithGroups(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedWithoutGroups")
fun View.showWhenFeedWithoutGroups(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedNavigation")
fun View.showWhenFeedNavigation(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedLoadFailed")
fun View.showWhenFeedLoadFailed(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoadFailed)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedEmpty")
fun View.showWhenFeedEmpty(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty)
    View.VISIBLE else View.INVISIBLE
}


