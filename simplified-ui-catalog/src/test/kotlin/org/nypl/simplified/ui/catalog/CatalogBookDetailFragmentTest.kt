package org.nypl.simplified.ui.catalog

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.listeners.api.FragmentListenerFinder
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import java.net.URI

@RunWith(AndroidJUnit4::class)
class CatalogBookDetailFragmentTest {
  private lateinit var scenario: FragmentScenario<CatalogBookDetailFragment>

  @MockK private lateinit var mockBookCoversProvider: BookCoverProviderType
  @MockK private lateinit var mockScreenInformation: ScreenSizeInformationType
  @MockK private lateinit var mockBuildConfigService: BuildConfigurationServiceType
  @MockK private lateinit var mockCatalogBorrowViewModel: CatalogBorrowViewModel
  @MockK private lateinit var mockCatalogBookDetailViewModel: CatalogBookDetailViewModel
  @MockK private lateinit var mockCatalogBookDetailEventListener: FragmentListenerType<CatalogBookDetailEvent>

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    // Mock service directory to return mocked services
    mockkObject(Services)
    every {
      Services.serviceDirectory().requireService(BookCoverProviderType::class.java)
    } returns mockBookCoversProvider
    every {
      Services.serviceDirectory().requireService(ScreenSizeInformationType::class.java)
    } returns mockScreenInformation
    every {
      Services.serviceDirectory().requireService(BuildConfigurationServiceType::class.java)
    } returns mockBuildConfigService

    // Mock ViewModel creation
    mockkConstructor(CatalogBorrowViewModelFactory::class)
    every {
      anyConstructed<CatalogBorrowViewModelFactory>().create(CatalogBorrowViewModel::class.java)
    } returns mockCatalogBorrowViewModel

    mockkConstructor(CatalogBookDetailViewModelFactory::class)
    every {
      anyConstructed<CatalogBookDetailViewModelFactory>().create(CatalogBookDetailViewModel::class.java)
    } returns mockCatalogBookDetailViewModel

    // Mock ListenerFinder rather than traversing upwards towards a stubbed listener "repository"
    mockkObject(FragmentListenerFinder)
    every {
      FragmentListenerFinder.findListener(any(), CatalogBookDetailEvent::class.java)
    } returns mockCatalogBookDetailEventListener

    val args = CatalogFeedArguments.CatalogFeedArgumentsRemote(
      "title",
      CatalogFeedOwnership.OwnedByAccount(AccountID.generate()),
      URI.create("feedUri"),
      false
    )
    val params = CatalogBookDetailFragmentParameters(
      CatalogTestUtils.buildTestFeedEntryOPDS(),
      args
    )

    scenario = launchFragmentInContainer(
      fragmentArgs = bundleOf(CatalogBookDetailFragment.PARAMETERS_ID to params),
      themeResId = R.style.SimplifiedTheme_NoActionBar
    )
  }

  @After
  fun tearDown() {
    scenario.close()
  }

  @Test
  fun `details show format label when required by build configuration`() {
    every { mockBuildConfigService.showFormatLabel } returns true

    scenario.recreate()

    onView(withId(R.id.bookDetailFormat)).check(matches(withText("eBook")))
  }

  @Test
  fun `details hides book format label when required by build configuration`() {
    every { mockBuildConfigService.showFormatLabel } returns false

    scenario.recreate()

    onView(withId(R.id.bookDetailFormat)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
  }
}
