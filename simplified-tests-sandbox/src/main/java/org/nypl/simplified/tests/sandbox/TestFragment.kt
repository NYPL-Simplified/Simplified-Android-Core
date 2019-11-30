package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pandora.bottomnavigator.BottomNavigator

class TestFragment : Fragment() {

  private lateinit var navigator: BottomNavigator
  private lateinit var tabView: BottomNavigationView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    val layout =
      inflater.inflate(R.layout.tabbed_host, container, false)

    this.tabView =
      layout.findViewById(R.id.bottomNavigator)

    this.navigator =
      BottomNavigator.onCreate(
        this.requireActivity(),
        rootFragmentsFactory = mapOf(
          R.id.tabCatalog to {
            RedFragment()
          },
          R.id.tabBooks to {
            GreenFragment()
          },
          R.id.tabHolds to {
            BlueFragment()
          },
          R.id.tabSettings to {
            YellowFragment()
          }
        ),
        defaultTab = R.id.tabCatalog,
        fragmentContainer = R.id.fragmentHolder,
        bottomNavigationView = this.tabView
      )

    return layout
  }
}