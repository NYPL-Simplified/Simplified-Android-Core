package org.nypl.simplified.viewer.epub.readium1.toc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ReaderTOCFragmentPagerAdapter(
  fragmentManager: FragmentManager,
  val tocParameters: ReaderTOCParameters
) : FragmentPagerAdapter(fragmentManager) {

  override fun getCount(): Int {
    return 2
  }

  override fun getPageTitle(position: Int): CharSequence? {
    return if (position > 0) {
      "Bookmarks"
    } else {
      "Contents"
    }
  }

  override fun getItem(position: Int): Fragment {
    return if (position > 0) {
      ReaderTOCBookmarksFragment.newInstance(this.tocParameters)
    } else {
      ReaderTOCContentsFragment.newInstance(this.tocParameters)
    }
  }
}
