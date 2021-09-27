package org.nypl.simplified.ui.profiles

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.profiles.api.ProfileReadableType

internal class ProfileViewHolder(
  private val parent: View,
  private val icon: ImageView,
  private val modify: ImageView,
  private val delete: ImageView,
  private val name: TextView
) : RecyclerView.ViewHolder(parent) {

  fun bindTo(
    profile: ProfileReadableType,
    onProfileSelected: (ProfileReadableType) -> Unit,
    onProfileModifyRequested: (ProfileReadableType) -> Unit,
    onProfileDeleteRequested: (ProfileReadableType) -> Unit
  ) {
    this.icon.setOnClickListener { onProfileSelected.invoke(profile) }
    this.name.setOnClickListener { onProfileSelected.invoke(profile) }
    this.name.text = profile.displayName
    this.modify.setOnClickListener { onProfileModifyRequested.invoke(profile) }
    this.delete.setOnClickListener { onProfileDeleteRequested.invoke(profile) }
  }
}
