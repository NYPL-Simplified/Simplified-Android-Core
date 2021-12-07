package org.nypl.simplified.ui.accounts

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.picasso.Picasso
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.ui.images.ImageLoaderType
import java.net.URI

@RunWith(AndroidJUnit4::class)
class AccountListAdapterTest {
  private lateinit var subject: AccountListAdapter
  private lateinit var viewHolder: AccountListAdapter.AccountViewHolder

  private val mockImageLoader: ImageLoaderType = mock()
  private val mockPicasso: Picasso = mock()
  private val mockOnItemClicked: (AccountType) -> Unit = mock()
  private val mockOnItemDeleteClicked: (AccountType) -> Unit = mock()

  @Before
  fun setUp() {
    whenever(mockImageLoader.loader).thenReturn(mockPicasso)

    subject = AccountListAdapter(
        mockImageLoader,
        mockOnItemClicked,
        mockOnItemDeleteClicked
    )

    val context = ApplicationProvider.getApplicationContext<Context>()
    context.setTheme(R.style.SimplifiedTheme_NoActionBar)

    viewHolder = subject.createViewHolder(LinearLayout(context), 0)
  }

  @Test
  fun `the options menu is hidden when there is only one account`() {
    subject.submitList(listOf(compositeMockAccount()))

    subject.onBindViewHolder(viewHolder, 0)
    viewHolder.binding.popupMenuIcon.visibility shouldBeEqualTo View.GONE
  }

  @Test
  fun `the options menu is visible when there is more than one account`() {
    subject.submitList(listOf(compositeMockAccount(), compositeMockAccount()))

    subject.onBindViewHolder(viewHolder, 0)
    viewHolder.binding.popupMenuIcon.visibility shouldBeEqualTo View.VISIBLE
  }

  // Not great
  private fun compositeMockAccount(): AccountType {
    val mockDescription: AccountProviderDescription = mock()

    val mockProvider: AccountProviderType = mock {
      on { toDescription() }.doReturn(
        mockDescription
      )
    }

    return mock {
      on { preferences }.doReturn(
        AccountPreferences(
          false,
          URI("someURI"),
          emptyList()
        )
      )
      on { provider }.doReturn(
        mockProvider
      )
    }
  }
}
