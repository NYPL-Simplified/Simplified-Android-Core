package org.nypl.simplified.ui.profiles

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.nypl.simplified.profiles.api.ProfileReadableType

internal class ProfileAdapter(
  private val onProfileSelected: (ProfileReadableType) -> Unit,
  private val onProfileModifyRequested: (ProfileReadableType) -> Unit,
  private val onProfileDeleteRequested: (ProfileReadableType) -> Unit
) : ListAdapter<ProfileReadableType, ProfileViewHolder>(ProfileDiff) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ProfileViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.profile_cell, parent, false)
    val icon =
      item.findViewById<ImageView>(R.id.profileImage)
    val modify =
      item.findViewById<ImageView>(R.id.profileModify)
    val delete =
      item.findViewById<ImageView>(R.id.profileDelete)
    val name =
      item.findViewById<TextView>(R.id.profileName)

    return ProfileViewHolder(
      parent = item,
      icon = icon,
      modify = modify,
      delete = delete,
      name = name
    )
  }

  override fun onBindViewHolder(
    holder: ProfileViewHolder,
    position: Int
  ) {
    holder.bindTo(
      profile = this.getItem(position),
      onProfileSelected = this.onProfileSelected,
      onProfileModifyRequested = this.onProfileModifyRequested,
      onProfileDeleteRequested = this.onProfileDeleteRequested
    )
  }

  object ProfileDiff : DiffUtil.ItemCallback<ProfileReadableType>() {

    override fun areItemsTheSame(oldItem: ProfileReadableType, newItem: ProfileReadableType): Boolean {
      return oldItem.id.compareTo(newItem.id) == 0
    }

    override fun areContentsTheSame(oldItem: ProfileReadableType, newItem: ProfileReadableType): Boolean {
      return oldItem.displayName == newItem.displayName
    }
  }
}
