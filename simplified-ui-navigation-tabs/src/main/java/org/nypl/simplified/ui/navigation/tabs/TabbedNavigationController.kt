package org.nypl.simplified.ui.navigation.tabs

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.io7m.junreachable.UnreachableCodeException
import com.pandora.bottomnavigator.BottomNavigator
import org.joda.time.DateTime
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting.SortBy.SORT_BY_TITLE
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountFragment
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountRegistryFragment
import org.nypl.simplified.ui.accounts.AccountsFragment
import org.nypl.simplified.ui.accounts.AccountsFragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.profiles.ProfileTabFragment
import org.nypl.simplified.ui.settings.SettingsConfigurationServiceType
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
  private val settingsConfiguration: SettingsConfigurationServiceType,
  private val profilesController: ProfilesControllerType,
  private val navigator: BottomNavigator
) : SettingsNavigationControllerType, CatalogNavigationControllerType {

  private val logger = LoggerFactory.getLogger(TabbedNavigationController::class.java)

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
      settingsConfiguration: SettingsConfigurationServiceType,
      @IdRes fragmentContainerId: Int,
      navigationView: BottomNavigationView
    ): TabbedNavigationController {

      this.logger.debug("creating bottom navigator")
      val navigator =
        BottomNavigator.onCreate(
          fragmentContainer = fragmentContainerId,
          bottomNavigationView = navigationView,
          rootFragmentsFactory = mapOf(
            R.id.tabCatalog to {
              this.createCatalogFragment(
                context = activity,
                id = R.id.tabCatalog,
                feedArguments = catalogFeedArguments(profilesController)
              )
            },
            R.id.tabBooks to {
              this.createBooksFragment(activity, R.id.tabBooks)
            },
            R.id.tabHolds to {
              this.createHoldsFragment(activity, R.id.tabBooks)
            },
            R.id.tabSettings to {
              this.createSettingsFragment(R.id.tabSettings)
            },
            R.id.tabProfile to {
              ProfileTabFragment()
            }
          ),
          defaultTab = R.id.tabCatalog,
          activity = activity
        )

      navigationView.itemIconTintList = this.colorStateListForTabs(activity)
      navigationView.itemTextColor = this.colorStateListForTabs(activity)
      navigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

      return TabbedNavigationController(
        navigator = navigator,
        profilesController = profilesController,
        settingsConfiguration = settingsConfiguration
      )
    }

    private fun currentAge(
      profilesController: ProfilesControllerType
    ): Int {
      return try {
        val profile = profilesController.profileCurrent()
        profile.preferences().dateOfBirth?.yearsOld(DateTime.now()) ?: 1
      } catch (e: Exception) {
        this.logger.error("could not retrieve profile age: ", e)
        1
      }
    }

    private fun pickDefaultAccount(
      profilesController: ProfilesControllerType
    ): AccountType {
      val profile = profilesController.profileCurrent()
      val mostRecentId = profile.preferences().mostRecentAccount
      if (mostRecentId != null) {
        try {
          return profile.account(mostRecentId)
        } catch (e: Exception) {
          this.logger.error("stale account: ", e)
        }
      }
      for (account in profile.accounts().values) {
        return account
      }
      throw UnreachableCodeException()
    }

    private fun catalogFeedArguments(
      profilesController: ProfilesControllerType
    ): CatalogFeedArguments.CatalogFeedArgumentsRemote {
      val age = this.currentAge(profilesController)
      val account = this.pickDefaultAccount(profilesController)
      return CatalogFeedArguments.CatalogFeedArgumentsRemote(
        title = account.provider.displayName,
        ownership = CatalogFeedOwnership.OwnedByAccount(account.id),
        feedURI = account.provider.catalogURIForAge(age),
        isSearchResults = false
      )
    }

    private fun colorStateListForTabs(context: FragmentActivity): ColorStateList {
      val states =
        arrayOf(
          intArrayOf(android.R.attr.state_checked),
          intArrayOf(-android.R.attr.state_checked)
        )

      val colors =
        intArrayOf(
          ThemeControl.resolveColorAttribute(context.theme, R.attr.colorPrimary),
          Color.DKGRAY
        )

      return ColorStateList(states, colors)
    }

    private fun createSettingsFragment(id: Int): Fragment {
      this.logger.debug("[{}]: creating settings fragment", id)
      return SettingsFragmentMain()
    }

    private fun createHoldsFragment(
      context: Context,
      id: Int
    ): Fragment {
      this.logger.debug("[{}]: creating holds fragment", id)
      return CatalogFragmentFeed.create(
        CatalogFeedArgumentsLocalBooks(
          filterAccount = null,
          ownership = CatalogFeedOwnership.CollectedFromAccounts,
          searchTerms = null,
          selection = FeedBooksSelection.BOOKS_FEED_HOLDS,
          sortBy = SORT_BY_TITLE,
          title = context.getString(R.string.tabHolds)
        )
      )
    }

    private fun createBooksFragment(
      context: Context,
      id: Int
    ): Fragment {
      this.logger.debug("[{}]: creating books fragment", id)
      return CatalogFragmentFeed.create(
        CatalogFeedArgumentsLocalBooks(
          filterAccount = null,
          ownership = CatalogFeedOwnership.CollectedFromAccounts,
          searchTerms = null,
          selection = FeedBooksSelection.BOOKS_FEED_LOANED,
          sortBy = SORT_BY_TITLE,
          title = context.getString(R.string.tabBooks)
        )
      )
    }

    private fun createCatalogFragment(
      context: Context,
      id: Int,
      feedArguments: CatalogFeedArguments.CatalogFeedArgumentsRemote
    ): Fragment {
      this.logger.debug("[{}]: creating catalog fragment", id)
      return CatalogFragmentFeed.create(feedArguments)
    }
  }

  override fun openSettingsAbout() {
  }

  override fun openSettingsAccounts() {
    this.navigator.addFragment(
      fragment = AccountsFragment.create(
        AccountsFragmentParameters(
          shouldShowLibraryRegistryMenu = this.settingsConfiguration.allowAccountsRegistryAccess
        )
      ),
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

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    this.navigator.addFragment(
      fragment = AccountFragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  override fun openBookDetail(
    feedArguments: CatalogFeedArguments,
    entry: FeedEntry.FeedEntryOPDS
  ) {
    val parameters =
      CatalogFragmentBookDetailParameters(
        feedEntry = entry,
        feedArguments = feedArguments
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

  override fun openSettingsAccountRegistry() {
    this.navigator.addFragment(
      fragment = AccountRegistryFragment(),
      tab = this.navigator.currentTab()
    )
  }

  override fun clearHistory() {
    this.logger.debug("clearing bottom navigator history")
    this.navigator.clearAll()
  }

  override fun openViewer(
    activity: Activity,
    book: Book,
    format: BookFormat
  ) {
    Viewers.openViewer(activity, book, format)
  }
}
