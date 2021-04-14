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
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting.SortBy.SORT_BY_TITLE
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountFragment
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListFragment
import org.nypl.simplified.ui.accounts.AccountListFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.profiles.ProfileTabFragment
import org.nypl.simplified.ui.settings.SettingsFragmentCustomOPDS
import org.nypl.simplified.ui.settings.SettingsFragmentDebug
import org.nypl.simplified.ui.settings.SettingsFragmentMain
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.viewer.api.Viewers
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * A tabbed navigation controller based on Pandora's BottomNavigator.
 *
 * @see BottomNavigator
 * @see BottomNavigationView
 */

class TabbedNavigationController private constructor(
  private val settingsConfiguration: BuildConfigurationServiceType,
  private val profilesController: ProfilesControllerType,
  private val navigator: BottomNavigator,
  private val listener: () -> Unit
) : SettingsNavigationControllerType, CatalogNavigationControllerType {

  private val logger = LoggerFactory.getLogger(TabbedNavigationController::class.java)

  private val infoStream = this.navigator.infoStream().subscribe { action ->
    this.logger.debug(action.toString())
    this.listener()
  }

  companion object {

    private val logger = LoggerFactory.getLogger(TabbedNavigationController::class.java)

    /**
     * Create a new tabbed navigation controller. The controller will load fragments into the
     * fragment container specified by [fragmentContainerId], using the Pandora BottomNavigator
     * view [navigationView].
     */

    fun create(
      activity: FragmentActivity,
      accountProviders: AccountProviderRegistryType,
      profilesController: ProfilesControllerType,
      settingsConfiguration: BuildConfigurationServiceType,
      @IdRes fragmentContainerId: Int,
      navigationView: BottomNavigationView,
      listener: () -> Unit
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
                feedArguments = catalogFeedArguments(
                  activity,
                  profilesController,
                  accountProviders.defaultProvider
                )
              )
            },
            R.id.tabBooks to {
              this.createBooksFragment(
                context = activity,
                id = R.id.tabBooks,
                profilesController = profilesController,
                settingsConfiguration = settingsConfiguration,
                defaultProvider = accountProviders.defaultProvider
              )
            },
            R.id.tabHolds to {
              this.createHoldsFragment(
                context = activity,
                id = R.id.tabHolds,
                profilesController = profilesController,
                settingsConfiguration = settingsConfiguration,
                defaultProvider = accountProviders.defaultProvider
              )
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
        settingsConfiguration = settingsConfiguration,
        profilesController = profilesController,
        listener = listener
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
      profilesController: ProfilesControllerType,
      defaultProvider: AccountProviderType
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

      val accounts = profile.accounts().values
      return when {
        accounts.size > 1 -> {
          // Return the first account created from a non-default provider
          accounts.first { it.provider.id != defaultProvider.id }
        }
        accounts.size == 1 -> {
          // Return the first account
          accounts.first()
        }
        else -> {
          // There should always be at least one account
          throw UnreachableCodeException()
        }
      }
    }

    private fun catalogFeedArguments(
      context: Context,
      profilesController: ProfilesControllerType,
      defaultProvider: AccountProviderType
    ): CatalogFeedArguments.CatalogFeedArgumentsRemote {
      val age = this.currentAge(profilesController)
      val account = this.pickDefaultAccount(profilesController, defaultProvider)
      return CatalogFeedArguments.CatalogFeedArgumentsRemote(
        ownership = CatalogFeedOwnership.OwnedByAccount(account.id),
        feedURI = account.catalogURIForAge(age),
        isSearchResults = false,
        title = context.getString(R.string.tabCatalog)
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
      id: Int,
      profilesController: ProfilesControllerType,
      settingsConfiguration: BuildConfigurationServiceType,
      defaultProvider: AccountProviderType
    ): Fragment {
      this.logger.debug("[{}]: creating holds fragment", id)

      /*
       * SIMPLY-2923: Filter by the default account until 'All' view is approved by UX.
       */

      val filterAccountId =
        if (settingsConfiguration.showBooksFromAllAccounts) {
          null
        } else {
          pickDefaultAccount(profilesController, defaultProvider).id
        }

      return CatalogFragmentFeed.create(
        CatalogFeedArgumentsLocalBooks(
          filterAccount = filterAccountId,
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
      id: Int,
      profilesController: ProfilesControllerType,
      settingsConfiguration: BuildConfigurationServiceType,
      defaultProvider: AccountProviderType
    ): Fragment {
      this.logger.debug("[{}]: creating books fragment", id)

      /*
       * SIMPLY-2923: Filter by the default account until 'All' view is approved by UX.
       */

      val filterAccountId =
        if (settingsConfiguration.showBooksFromAllAccounts) {
          null
        } else {
          pickDefaultAccount(profilesController, defaultProvider).id
        }

      return CatalogFragmentFeed.create(
        CatalogFeedArgumentsLocalBooks(
          filterAccount = filterAccountId,
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
    throw NotImplementedError()
  }

  override fun openSettingsAccounts() {
    this.navigator.addFragment(
      fragment = AccountListFragment.create(
        AccountListFragmentParameters(
          shouldShowLibraryRegistryMenu = this.settingsConfiguration.allowAccountsRegistryAccess
        )
      ),
      tab = R.id.tabSettings
    )
  }

  override fun openSettingsAcknowledgements() {
    throw NotImplementedError()
  }

  override fun openSettingsEULA() {
    throw NotImplementedError()
  }

  override fun openSettingsFaq() {
    throw NotImplementedError()
  }

  override fun openSettingsLicense() {
    throw NotImplementedError()
  }

  override fun openSettingsVersion() {
    this.navigator.addFragment(
      fragment = SettingsFragmentDebug(),
      tab = R.id.tabSettings
    )
  }

  override fun popBackStack(): Boolean {
    return this.navigator.pop()
  }

  override fun popToRoot(): Boolean {
    val isAtRootOfStack = (1 == this.navigator.currentStackSize())
    if (isAtRootOfStack) {
      return false // Nothing to do
    }
    val currentTab = this.navigator.currentTab()
    this.navigator.reset(currentTab, false)
    return true
  }

  override fun openSettingsCustomOPDS() {
    this.navigator.addFragment(
      fragment = SettingsFragmentCustomOPDS(),
      tab = R.id.tabSettings
    )
  }

  override fun openErrorPage(parameters: ErrorPageParameters) {
    this.navigator.addFragment(
      fragment = ErrorPageFragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  override fun backStackSize(): Int {
    // Note: currentStackSize() is not safe to call here as it may throw an NPE.
    return this.navigator.stackSize(this.navigator.currentTab()) - 1
  }

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    this.navigator.addFragment(
      fragment = AccountFragment.create(parameters),
      tab = R.id.tabSettings
    )
  }

  override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
    this.navigator.addFragment(
      fragment = AccountSAML20Fragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  override fun openBookDetail(
    feedArguments: CatalogFeedArguments,
    entry: FeedEntry.FeedEntryOPDS
  ) {
    this.navigator.addFragment(
      fragment = CatalogFragmentBookDetail.create(
        CatalogFragmentBookDetailParameters(
          feedEntry = entry,
          feedArguments = feedArguments
        )
      ),
      tab = this.navigator.currentTab()
    )
  }

  override fun openBookDownloadLogin(
    bookID: BookID,
    downloadURI: URI
  ) {
    this.navigator.addFragment(
      fragment = CatalogSAML20Fragment.create(
        CatalogSAML20FragmentParameters(
          bookID = bookID,
          downloadURI = downloadURI
        )
      ),
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
      fragment = AccountListRegistryFragment(),
      tab = R.id.tabSettings
    )
  }

  override fun openCatalogAfterAuthentication() {
    this.navigator.reset(R.id.tabCatalog, false)
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
    /*
     * XXX: Enable or disable support for R2 based on the current profile's preferences. When R2
     * moves from being experimental, this code can be removed.
     */

    val profile =
      this.profilesController.profileCurrent()
    val viewerPreferences =
      ViewerPreferences(
        flags = mapOf(Pair("useExperimentalR2", profile.preferences().useExperimentalR2))
      )

    Viewers.openViewer(
      activity = activity,
      preferences = viewerPreferences,
      book = book,
      format = format
    )
  }
}
