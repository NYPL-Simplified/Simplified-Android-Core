package org.nypl.simplified.ui.profiles

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.profiles.api.ProfileReadableType

class ProfileAdapter(
  private val profiles: List<ProfileReadableType>,
  private val onProfileSelected: (ProfileReadableType) -> Unit,
  private val onProfileModifyRequested: (ProfileReadableType) -> Unit,
  private val onProfileDeleteRequested: (ProfileReadableType) -> Unit
) : RecyclerView.Adapter<ProfileViewHolder>() {

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

  override fun getItemCount(): Int {
    return this.profiles.size
  }

  override fun onBindViewHolder(
    holder: ProfileViewHolder,
    position: Int
  ) {
    holder.bindTo(
      profile = this.profiles[position],
      onProfileSelected = this.onProfileSelected,
      onProfileModifyRequested = this.onProfileModifyRequested,
      onProfileDeleteRequested = this.onProfileDeleteRequested
    )
  }
}
