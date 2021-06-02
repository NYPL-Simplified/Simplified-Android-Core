package org.nypl.simplified.ui.navigation.tabs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.io7m.junreachable.UnreachableCodeException
import com.pandora.bottomnavigator.BottomNavigator
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership
import org.nypl.simplified.ui.catalog.CatalogFeedFragment
import org.nypl.simplified.ui.profiles.ProfileTabFragment
import org.nypl.simplified.ui.settings.SettingsMainFragment
import org.nypl.simplified.ui.theme.ThemeControl
import org.slf4j.LoggerFactory

object BottomNavigators {

  private val logger = LoggerFactory.getLogger(BottomNavigators::class.java)

  /**
   * Create a new tabbed navigation controller. The controller will load fragments into the
   * fragment container specified by [fragmentContainerId], using the Pandora BottomNavigator
   * view [navigationView].
   */

  fun create(
    fragment: Fragment,
    @IdRes fragmentContainerId: Int,
    navigationView: BottomNavigationView,
    accountProviders: AccountProviderRegistryType,
    profilesController: ProfilesControllerType,
    settingsConfiguration: BuildConfigurationServiceType,
  ): BottomNavigator {
    logger.debug("creating bottom navigator")

    val context =
      fragment.requireContext()

    val navigator =
      BottomNavigator.onCreate(
        fragmentContainer = fragmentContainerId,
        bottomNavigationView = navigationView,
        rootFragmentsFactory = mapOf(
          R.id.tabCatalog to {
            createCatalogFragment(
              context = context,
              id = R.id.tabCatalog,
              feedArguments = catalogFeedArguments(
                context,
                profilesController,
                accountProviders.defaultProvider
              )
            )
          },
          R.id.tabBooks to {
            createBooksFragment(
              context = context,
              id = R.id.tabBooks,
              profilesController = profilesController,
              settingsConfiguration = settingsConfiguration,
              defaultProvider = accountProviders.defaultProvider
            )
          },
          R.id.tabHolds to {
            createHoldsFragment(
              context = context,
              id = R.id.tabHolds,
              profilesController = profilesController,
              settingsConfiguration = settingsConfiguration,
              defaultProvider = accountProviders.defaultProvider
            )
          },
          R.id.tabSettings to {
            createSettingsFragment(R.id.tabSettings)
          },
          R.id.tabProfile to {
            ProfileTabFragment()
          }
        ),
        defaultTab = R.id.tabCatalog,
        fragment = fragment,
        instanceOwner = fragment
      )

    navigationView.itemIconTintList = colorStateListForTabs(context)
    navigationView.itemTextColor = colorStateListForTabs(context)
    navigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

    return navigator
  }

  private fun currentAge(
    profilesController: ProfilesControllerType
  ): Int {
    return try {
      val profile = profilesController.profileCurrent()
      profile.preferences().dateOfBirth?.yearsOld(DateTime.now()) ?: 1
    } catch (e: Exception) {
      logger.error("could not retrieve profile age: ", e)
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
        logger.error("stale account: ", e)
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
    val age = currentAge(profilesController)
    val account = pickDefaultAccount(profilesController, defaultProvider)
    return CatalogFeedArguments.CatalogFeedArgumentsRemote(
      ownership = CatalogFeedOwnership.OwnedByAccount(account.id),
      feedURI = account.catalogURIForAge(age),
      isSearchResults = false,
      title = context.getString(R.string.tabCatalog)
    )
  }

  private fun colorStateListForTabs(context: Context): ColorStateList {
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
    logger.debug("[{}]: creating settings fragment", id)
    return SettingsMainFragment()
  }

  private fun createHoldsFragment(
    context: Context,
    id: Int,
    profilesController: ProfilesControllerType,
    settingsConfiguration: BuildConfigurationServiceType,
    defaultProvider: AccountProviderType
  ): Fragment {
    logger.debug("[{}]: creating holds fragment", id)

    /*
     * SIMPLY-2923: Filter by the default account until 'All' view is approved by UX.
     */

    val filterAccountId =
      if (settingsConfiguration.showBooksFromAllAccounts) {
        null
      } else {
        pickDefaultAccount(profilesController, defaultProvider).id
      }

    return CatalogFeedFragment.create(
      CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
        filterAccount = filterAccountId,
        ownership = CatalogFeedOwnership.CollectedFromAccounts,
        searchTerms = null,
        selection = FeedBooksSelection.BOOKS_FEED_HOLDS,
        sortBy = FeedFacet.FeedFacetPseudo.Sorting.SortBy.SORT_BY_TITLE,
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
    logger.debug("[{}]: creating books fragment", id)

    /*
     * SIMPLY-2923: Filter by the default account until 'All' view is approved by UX.
     */

    val filterAccountId =
      if (settingsConfiguration.showBooksFromAllAccounts) {
        null
      } else {
        pickDefaultAccount(profilesController, defaultProvider).id
      }

    return CatalogFeedFragment.create(
      CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
        filterAccount = filterAccountId,
        ownership = CatalogFeedOwnership.CollectedFromAccounts,
        searchTerms = null,
        selection = FeedBooksSelection.BOOKS_FEED_LOANED,
        sortBy = FeedFacet.FeedFacetPseudo.Sorting.SortBy.SORT_BY_TITLE,
        title = context.getString(R.string.tabBooks)
      )
    )
  }

  private fun createCatalogFragment(
    context: Context,
    id: Int,
    feedArguments: CatalogFeedArguments.CatalogFeedArgumentsRemote
  ): Fragment {
    logger.debug("[{}]: creating catalog fragment", id)
    return CatalogFeedFragment.create(feedArguments)
  }
}
