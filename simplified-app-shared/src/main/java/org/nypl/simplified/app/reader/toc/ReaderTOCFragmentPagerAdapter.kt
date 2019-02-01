package org.nypl.simplified.app.reader.toc

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class ReaderTOCFragmentPagerAdapter(
  fragmentManager: FragmentManager,
  val tocParameters: ReaderTOCParameters) : FragmentPagerAdapter(fragmentManager) {

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
