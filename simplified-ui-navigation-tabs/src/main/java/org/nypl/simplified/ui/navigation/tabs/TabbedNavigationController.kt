package org.nypl.simplified.ui.navigation.tabs

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.pandora.bottomnavigator.BottomNavigator
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.settings.SettingsFragmentAccount
import org.nypl.simplified.ui.settings.SettingsFragmentAccountParameters
import org.nypl.simplified.ui.settings.SettingsFragmentAccounts
import org.nypl.simplified.ui.settings.SettingsFragmentCustomOPDS
import org.nypl.simplified.ui.settings.SettingsFragmentMain
import org.nypl.simplified.ui.settings.SettingsFragmentVersion
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.viewer.api.Viewers
import org.slf4j.LoggerFactory

/**
 * A tabbed navigation controller based on Pandora's BottomNavigator.
 *
 * @see BottomNavigator
 * @see BottomNavigationView
 */

class TabbedNavigationController private constructor(
  private val profilesController: ProfilesControllerType,
  private val navigator: BottomNavigator
) : SettingsNavigationControllerType, CatalogNavigationControllerType {

  companion object {

    private val logger = LoggerFactory.getLogger(TabbedNavigationController::class.java)

    /**
     * Create a new tabbed navigation controller. The controller will load fragments into the
     * fragment container specified by [fragmentContainerId], using the Pandora BottomNavigator
     * view [navigationView].
     */

    fun create(
      activity: FragmentActivity,
      profilesController: ProfilesControllerType,
      @IdRes fragmentContainerId: Int,
      navigationView: BottomNavigationView
    ): TabbedNavigationController {

      val navigator =
        BottomNavigator.onCreate(
          fragmentContainer = fragmentContainerId,
          bottomNavigationView = navigationView,
          rootFragmentsFactory = mapOf(
            R.id.tabCatalog to {
              this.createCatalogFragment(profilesController, R.id.tabCatalog)
            },
            R.id.tabBooks to {
              this.createBooksFragment(activity, profilesController, R.id.tabBooks)
            },
            R.id.tabHolds to {
              this.createHoldsFragment(activity, profilesController, R.id.tabBooks)
            },
            R.id.tabSettings to {
              this.createSettingsFragment(R.id.tabSettings)
            }
          ),
          defaultTab = R.id.tabCatalog,
          activity = activity
        )

      navigationView.itemIconTintList = this.colorStateListForTabs(activity)
      navigationView.itemTextColor = this.colorStateListForTabs(activity)
      navigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

      return TabbedNavigationController(
        profilesController = profilesController,
        navigator = navigator
      )
    }

    private fun colorStateListForTabs(context: FragmentActivity): ColorStateList {
      val states =
        arrayOf(
          intArrayOf(android.R.attr.state_checked),
          intArrayOf(-android.R.attr.state_checked))

      val colors =
        intArrayOf(
          ThemeControl.resolveColorAttribute(context.theme, R.attr.colorPrimary),
          Color.DKGRAY)

      return ColorStateList(states, colors)
    }

    private fun createSettingsFragment(id: Int): Fragment {
      this.logger.debug("[{}]: creating settings fragment", id)
      return SettingsFragmentMain()
    }

    private fun createHoldsFragment(
      context: Context,
      profilesController: ProfilesControllerType,
      id: Int
    ): Fragment {
      this.logger.debug("[{}]: creating holds fragment", id)

      val account = profilesController.profileAccountCurrent()
      return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
        title = context.getString(R.string.tabHolds),
        facetType = FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE,
        searchTerms = null,
        selection = FeedBooksSelection.BOOKS_FEED_HOLDS,
        accountId = account.id
      ))
    }

    private fun createBooksFragment(
      context: Context,
      profilesController: ProfilesControllerType,
      id: Int
    ): Fragment {
      this.logger.debug("[{}]: creating books fragment", id)

      val account = profilesController.profileAccountCurrent()
      return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
        title = context.getString(R.string.tabBooks),
        facetType = FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE,
        searchTerms = null,
        selection = FeedBooksSelection.BOOKS_FEED_LOANED,
        accountId = account.id
      ))
    }

    private fun createCatalogFragment(
      profilesController: ProfilesControllerType,
      id: Int
    ): Fragment {
      this.logger.debug("[{}]: creating catalog fragment", id)

      val account = profilesController.profileAccountCurrent()
      return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsRemote(
        title = account.provider.displayName,
        feedURI = account.provider.catalogURI,
        isSearchResults = false,
        accountId = account.id
      ))
    }
  }

  override fun openSettingsAbout() {

  }

  override fun openSettingsAccount(id: AccountID) {
    this.navigator.addFragment(
      fragment = SettingsFragmentAccount.create(SettingsFragmentAccountParameters(id)),
      tab = this.navigator.currentTab()
    )
  }

  override fun openSettingsAccounts() {
    this.navigator.addFragment(
      fragment = SettingsFragmentAccounts(),
      tab = this.navigator.currentTab()
    )
  }

  override fun openSettingsAcknowledgements() {

  }

  override fun openSettingsEULA() {

  }

  override fun openSettingsFaq() {

  }

  override fun openSettingsLicense() {

  }

  override fun openSettingsVersion() {
    this.navigator.addFragment(
      fragment = SettingsFragmentVersion(),
      tab = this.navigator.currentTab()
    )
  }

  override fun popBackStack(): Boolean {
    return this.navigator.pop()
  }

  override fun openSettingsCustomOPDS() {
    this.navigator.addFragment(
      fragment = SettingsFragmentCustomOPDS(),
      tab = this.navigator.currentTab()
    )
  }

  override fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>) {
    this.navigator.addFragment(
      fragment = ErrorPageFragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  override fun backStackSize(): Int {
    return this.navigator.stackSize(this.navigator.currentTab())
  }

  override fun openBookDetail(entry: FeedEntry.FeedEntryOPDS) {
    val parameters =
      CatalogFragmentBookDetailParameters(
        accountId = this.profilesController.profileAccountCurrent().id,
        feedEntry = entry
      )

    this.navigator.addFragment(
      fragment = CatalogFragmentBookDetail.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  override fun openFeed(feedArguments: CatalogFeedArguments) {
    this.navigator.addFragment(
      fragment = CatalogFragmentFeed.create(feedArguments),
      tab = this.navigator.currentTab()
    )
  }

  override fun openViewer(
    activity: Activity,
    book: Book,
    format: BookFormat
  ) {
    Viewers.openViewer(activity, book, format)
  }
}
