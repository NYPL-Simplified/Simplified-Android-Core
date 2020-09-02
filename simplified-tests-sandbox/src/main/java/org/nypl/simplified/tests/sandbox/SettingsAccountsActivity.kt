package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginConnectionFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigOAuthScheme
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.tests.MockBooksController
import org.nypl.simplified.tests.MockDocumentStore
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.ui.accounts.AccountsFragment
import org.nypl.simplified.ui.accounts.AccountsFragmentParameters
import org.nypl.simplified.ui.images.ImageLoader
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import java.io.IOException
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class SettingsAccountsActivity : AppCompatActivity(), ServiceDirectoryProviderType, ToolbarHostType {

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var account: MockAccount
  private lateinit var services: MutableServiceDirectory
  private lateinit var fragment: Fragment
  private lateinit var registry: BookRegistryType
  private val executor = Executors.newSingleThreadScheduledExecutor()

  override fun serviceDirectory() =
    this.services

  private val credentials =
    AccountAuthenticationCredentials.Basic(
      userName = AccountUsername("Leaving!"),
      password = AccountPassword("a very strong password"),
      adobeCredentials = null,
      authenticationDescription = null
    )

  private var stateIndex = 0
  private val states =
    listOf(
      this.loggedOut(),
      this.loggingInCancellable(),
      this.loggingInNotCancellable(),
      this.loggingInWaitingForExternal(),
      this.loginFailed(),
      this.loggedIn(),
      this.loggingOut(),
      this.logoutFailed()
    )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.fragment_host)

    this.services = MutableServiceDirectory()
    val buildConfiguration = object : BuildConfigurationServiceType {
      override val allowAccountsAccess: Boolean
        get() = true
      override val allowAccountsRegistryAccess: Boolean
        get() = true
      override val showDebugBookDetailStatus: Boolean
        get() = true
      override val showSettingsTab: Boolean
        get() = true
      override val showHoldsTab: Boolean
        get() = true
      override val vcsCommit: String =
        "abcd"
      override val simplifiedVersion: String
        get() = "zyxw"
      override val supportErrorReportEmailAddress: String
        get() = "errors@example.com"
      override val supportErrorReportSubject: String
        get() = "[error]"
      override val oauthCallbackScheme: BuildConfigOAuthScheme =
        BuildConfigOAuthScheme("simplified-sandbox-oauth")
    }
    val books = MockBooksController()
    val documents = MockDocumentStore()
    val imageLoader = ImageLoader.create(this)
    val profiles = MockProfilesController(1, 1)
    this.accountEvents = profiles.accountEventSource

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
    this.account = profile.accounts()[profile.accounts().firstKey()] as MockAccount
    this.account.loginStateMutable = this.states[0]

    this.fragment =
      AccountsFragment.create(
        AccountsFragmentParameters(
          shouldShowLibraryRegistryMenu = false
        )
      )

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.fragmentHolder, this.fragment, "MAIN")
      .commit()
  }

  private fun loggedOut(): AccountLoginState {
    return AccountNotLoggedIn
  }

  private fun loggingInCancellable(): AccountLoginState {
    return AccountLoggingIn(
      status = "Logging in (Cancellable)",
      description = AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Login",
        authenticate = URI.create("urn:unused"),
        logoURI = null
      ),
      cancellable = true
    )
  }

  private fun loggingInWaitingForExternal(): AccountLoginState {
    return AccountLoggingInWaitingForExternalAuthentication(
      description = AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Login",
        authenticate = URI.create("urn:unused"),
        logoURI = null
      ),
      status = "Logging in (Waiting for external)"
    )
  }

  private fun loggingInNotCancellable(): AccountLoginState {
    return AccountLoggingIn(
      status = "Logging in (Not cancellable)",
      description = AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Login",
        authenticate = URI.create("urn:unused"),
        logoURI = null
      ),
      cancellable = false
    )
  }

  private fun loggingOut(): AccountLoginState {
    return AccountLoggingOut(
      status = "Logging out",
      credentials = this.credentials
    )
  }

  private fun loggedIn(): AccountLoginState {
    return AccountLoggedIn(
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("pass"),
        adobeCredentials = null,
        authenticationDescription = null
      )
    )
  }

  private fun loginFailed(): AccountLoginState {
    val recorder =
      TaskRecorder.create<AccountLoginErrorData>()
    recorder.beginNewStep("Started")
    recorder.currentStepFailed("Failed", AccountLoginConnectionFailure("Failed!"), IOException())
    return AccountLoginFailed(recorder.finishFailure<String>())
  }

  private fun logoutFailed(): AccountLoginState {
    val recorder =
      TaskRecorder.create<AccountLogoutErrorData>()
    recorder.beginNewStep("Started")
    recorder.currentStepFailed(
      "Failed",
      AccountLogoutErrorData.AccountLogoutUnexpectedException(IOException()),
      IOException()
    )
    return AccountLogoutFailed(recorder.finishFailure<String>(), this.credentials)
  }

  override fun onStart() {
    super.onStart()

    val uiThread = object : UIThreadServiceType {
    }

    this.executor.scheduleAtFixedRate(
      {
        // this.cycleStates(uiThread)
      },
      1L,
      1L,
      SECONDS
    )
  }

  private fun cycleStates(uiThread: UIThreadServiceType) {
    uiThread.runOnUIThread {
      val state = this.states[this.stateIndex]
      this.account.loginStateMutable = state
      this.accountEvents.onNext(
        AccountEventLoginStateChanged(state.toString(), this.account.id, state)
      )
      this.stateIndex = (this.stateIndex + 1) % this.states.size
    }
  }

  override fun onStop() {
    super.onStop()
  }

  override fun findToolbar(): Toolbar {
    return this.findViewById(R.id.toolbar)
  }
}
