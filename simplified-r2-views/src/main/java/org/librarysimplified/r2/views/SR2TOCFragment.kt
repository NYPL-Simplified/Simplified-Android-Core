package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.librarysimplified.r2.views.internal.SR2TOCAdapter
import org.librarysimplified.r2.views.internal.SR2TOCBookmarksFragment
import org.librarysimplified.r2.views.internal.SR2TOCChaptersFragment
import org.librarysimplified.r2.views.internal.SR2TOCPage

class SR2TOCFragment : Fragment() {

  private lateinit var viewPagerAdapter: SR2TOCAdapter
  private lateinit var viewPager: ViewPager2
  private lateinit var tabLayout: TabLayout

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_table_of_contents, container, false)

    this.tabLayout =
      view.findViewById(R.id.tocTabs)
    this.viewPager =
      view.findViewById(R.id.tocViewPager)
    this.viewPagerAdapter =
      SR2TOCAdapter(
        fragment = this,
        pages = listOf(
          SR2TOCPage(
            title = this.resources.getString(R.string.tocTitle),
            fragmentConstructor = { SR2TOCChaptersFragment() }
          ),
          SR2TOCPage(
            title = this.resources.getString(R.string.tocBookmarks),
            fragmentConstructor = { SR2TOCBookmarksFragment() }
          )
        )
      )

    this.viewPager.adapter = this.viewPagerAdapter

    TabLayoutMediator(this.tabLayout, this.viewPager) { tab, position ->
      tab.text = this.viewPagerAdapter.titleOf(position)
    }.attach()

    return view
  }
}
