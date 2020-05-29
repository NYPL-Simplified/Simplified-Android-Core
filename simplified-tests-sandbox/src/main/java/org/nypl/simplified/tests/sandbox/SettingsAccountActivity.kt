package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginConnectionFailure
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.tests.MockBooksController
import org.nypl.simplified.tests.MockDocumentStore
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.ui.images.ImageLoader
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.settings.SettingsFragmentAccount
import org.nypl.simplified.ui.settings.SettingsFragmentAccountParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import java.io.IOException

class SettingsAccountActivity : AppCompatActivity(), ServiceDirectoryProviderType, ToolbarHostType {

  private lateinit var services: MutableServiceDirectory
  private lateinit var fragment: Fragment
  private lateinit var registry: BookRegistryType

  override fun serviceDirectory() = this.services

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.fragment_host)

    this.services = MutableServiceDirectory()
    val buildConfiguration = object : BuildConfigurationServiceType {
      override val vcsCommit: String = "abcd"
      override val errorReportEmail: String = "errors@example.com"
    }
    val books = MockBooksController()
    val documents = MockDocumentStore()
    val imageLoader = ImageLoader.create(this)
    val profiles = MockProfilesController(1, 1)

    this.registry = BookRegistry.create()
    this.services.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})
    this.services.putService(
      ScreenSizeInformationType::class.java,
      ScreenSizeInformation(this.resources)
    )
    this.services.putService(BuildConfigurationServiceType::class.java, buildConfiguration)
    this.services.putService(ProfilesControllerType::class.java, profiles)
    this.services.putService(DocumentStoreType::class.java, documents)
    this.services.putService(BooksControllerType::class.java, books)
    this.services.putService(ImageLoaderType::class.java, imageLoader)
    Services.initialize(this.services)

    val profile = profiles.profileCurrent()
    val account = profile.accounts()[profile.accounts().firstKey()] as MockAccount

    account.loginStateMutable = loginNot()

    this.fragment =
      SettingsFragmentAccount.create(
        SettingsFragmentAccountParameters(
          account.id
        )
      )

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.fragmentHolder, this.fragment, "MAIN")
      .commit()
  }

  private fun loginNot(): AccountLoginState {
    return AccountLoginState.AccountNotLoggedIn
  }

  private fun loggedIn(): AccountLoginState {
    return AccountLoginState.AccountLoggedIn(
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("abcd"),
        AccountBarcode.create("abcd")
      ).build()
    )
  }

  private fun loginFailed(): AccountLoginState {
    val recorder =
      TaskRecorder.create<AccountLoginState.AccountLoginErrorData>()
    recorder.beginNewStep("Started")
    recorder.currentStepFailed("Failed", AccountLoginConnectionFailure("Failed!"), IOException())
    val state = AccountLoginState.AccountLoginFailed(recorder.finishFailure<String>())
    return state
  }

  override fun onStart() {
    super.onStart()
  }

  override fun onStop() {
    super.onStop()
  }

  override fun findToolbar(): Toolbar {
    return this.findViewById(R.id.toolbar)
  }
}
