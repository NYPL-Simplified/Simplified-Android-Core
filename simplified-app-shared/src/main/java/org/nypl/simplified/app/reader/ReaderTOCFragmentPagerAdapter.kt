package org.nypl.simplified.app.reader

import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentManager
import android.support.v4.app.Fragment

class ReaderTOCFragmentPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

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
      ReaderTOCBookmarksFragment()
    } else {
      ReaderTOCContentsFragment()
    }
  }
}
