package org.librarysimplified.r2.views.internal

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class SR2TOCAdapter(
  fragment: Fragment,
  private val pages: List<SR2TOCPage>
) : FragmentStateAdapter(fragment) {

  override fun getItemCount(): Int {
    return this.pages.size
  }

  fun titleOf(position: Int): String =
    this.pages[position].title

  override fun createFragment(position: Int): Fragment {
    require(position >= 0) { "Position $position must be non-negative" }
    return this.pages[position].fragmentConstructor()
  }
}
