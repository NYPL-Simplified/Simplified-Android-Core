package org.nypl.simplified.ui.catalog

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("showWhenFeedLoading")
internal fun View.showWhenFeedLoading(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoading)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedWithGroups")
internal fun View.showWhenFeedWithGroups(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedWithoutGroups")
internal fun View.showWhenFeedWithoutGroups(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedNavigation")
internal fun View.showWhenFeedNavigation(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedLoadFailed")
internal fun View.showWhenFeedLoadFailed(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoadFailed)
    View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("showWhenFeedEmpty")
internal fun View.showWhenFeedEmpty(feedState: CatalogFeedState) {
  visibility = if (feedState is CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty)
    View.VISIBLE else View.INVISIBLE
}
