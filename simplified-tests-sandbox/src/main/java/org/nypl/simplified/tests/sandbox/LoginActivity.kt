package org.nypl.simplified.tests.sandbox

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.OptionType
import org.nypl.audiobook.android.tests.sandbox.R
import org.nypl.simplified.app.BundledContentResolver
import org.nypl.simplified.app.login.LoginDialog
import org.nypl.simplified.app.login.LoginDialogListenerType
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentialsStore
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.books.accounts.AccountBundledCredentialsEmpty
import org.nypl.simplified.books.accounts.AccountBundledCredentialsType
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventLoginStateChanged
import org.nypl.simplified.books.accounts.AccountLoginState
import org.nypl.simplified.books.accounts.AccountProviderCollection
import org.nypl.simplified.books.accounts.AccountProvidersJSON
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.accounts.AccountsDatabases
import org.nypl.simplified.books.analytics.AnalyticsLogger
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.feeds.FeedHTTPTransport
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfilesDatabase
import org.nypl.simplified.books.profiles.ProfilesDatabaseType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.opds.core.OPDSSearchParserType
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity(), LoginDialogListenerType {

  private lateinit var profiles: Controller
  private lateinit var timerExecutor: ListeningExecutorService
  private lateinit var analyticsLogger: AnalyticsLogger
  private lateinit var accountCredentials: AccountAuthenticationCredentialsStoreType
  private lateinit var accountBundledCredentials: AccountBundledCredentialsType
  private lateinit var accountProviders: AccountProviderCollection
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var feedTransport: OPDSFeedTransportType<OptionType<HTTPAuthType>>
  private lateinit var searchParser: OPDSSearchParserType
  private lateinit var profilesDatabase: ProfilesDatabaseType
  private lateinit var downloader: DownloaderType
  private lateinit var feedExecutor: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var feedParser: OPDSFeedParserType
  private lateinit var http: HTTPType
  private lateinit var readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>
  private lateinit var profileEvents: ObservableType<ProfileEvent>
  private lateinit var accountEvents: ObservableType<AccountEvent>
  private lateinit var executor: ListeningExecutorService

  override fun onLoginDialogWantsProfilesController(): ProfilesControllerType =
    this.profiles

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setTheme(R.style.SimplifiedTheme_ActionBar_Blue)
    this.setContentView(R.layout.empty)

    val layout =
      this.findViewById<ViewGroup>(R.id.empty)

    this.executor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    this.feedExecutor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    this.timerExecutor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    this.accountEvents =
      Observable.create<AccountEvent>()
    this.profileEvents =
      Observable.create<ProfileEvent>()
    this.readerBookmarkEvents =
      Observable.create()
    this.http =
      HTTP.newHTTP()
    this.feedParser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes()))
    this.feedTransport =
      FeedHTTPTransport.newTransport(this.http)
    this.bookRegistry =
      BookRegistry.create()
    this.bundledContent =
      BundledContentResolver.create(this.assets)
    this.searchParser =
      OPDSSearchParser.newParser()

    this.feedLoader =
      FeedLoader.create(
        exec = this.feedExecutor,
        parser = this.feedParser,
        searchParser = this.searchParser,
        transport = this.feedTransport,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent)

    this.downloader =
      DownloaderHTTP.newDownloader(
        this.feedExecutor,
        this.cacheDir,
        this.http)

    this.accountProviders =
      this.assets.open("Accounts.json").use { stream ->
        AccountProvidersJSON.deserializeFromStream(stream)
      }

    this.accountBundledCredentials =
      AccountBundledCredentialsEmpty.getInstance()

    this.accountCredentials =
      AccountAuthenticationCredentialsStore.open(
        File(this.filesDir, "credentials.json"),
        File(this.filesDir, "credentials.json.tmp"))

    this.profilesDatabase =
      ProfilesDatabase.openWithAnonymousProfileEnabled(
        this,
        this.accountEvents,
        this.accountProviders,
        this.accountBundledCredentials,
        this.accountCredentials,
        AccountsDatabases,
        this.accountProviders.providerDefault(),
        File(this.filesDir, "profiles"))

    this.analyticsLogger =
      AnalyticsLogger.create(File(this.filesDir, "analytics"))

    this.profiles =
      Controller.create(
        exec = this.executor,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        readerBookmarkEvents = this.readerBookmarkEvents,
        http = this.http,
        feedParser = this.feedParser,
        feedLoader = this.feedLoader,
        downloader = this.downloader,
        profiles = this.profilesDatabase,
        analyticsLogger = this.analyticsLogger,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        accountProviders = FunctionType { this.getAccountProviders() },
        timerExecutor = this.timerExecutor,
        adobeDrm = null
      )

    val button0 = Button(this)
    button0.text = "Login"
    button0.setOnClickListener {
      val loginDialog = LoginDialog()
      loginDialog.show(this.supportFragmentManager, "login-dialog")
    }

    val div0 = View(this)
    div0.layoutParams = LinearLayout.LayoutParams(16, 16)

    val button1 = Button(this)
    button1.text = "Logout"
    button1.setOnClickListener {
      this.profiles.profileAccountLogout(this.profiles.profileAccountCurrent().id())
    }

    val div1 = View(this)
    div1.layoutParams = LinearLayout.LayoutParams(16, 16)

    val items =
      arrayListOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten")

    val listView = ListView(this)
    listView.adapter =
      ExampleAdapter(
        context = this,
        onShowLoginDialog = {
          val loginDialog = LoginDialog()
          loginDialog.show(this.supportFragmentManager, "login-dialog")
        },
        controller = this.profiles,
        data = items
      )

    layout.addView(button0)
    layout.addView(div0)
    layout.addView(button1)
    layout.addView(div1)
    layout.addView(listView)
  }

  class ExampleViewHolder {
    lateinit var button: ExampleButton
  }

  class ExampleAdapter(
    context: Context,
    private val onShowLoginDialog: () -> (Unit),
    private val controller: ProfilesControllerType,
    data: ArrayList<String>) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_2, data) {

    override fun getView(
      position: Int,
      convertView: View?,
      parent: ViewGroup?): View {

      var inputView = convertView as ViewGroup?
      val item = this.getItem(position)
      val viewHolder =
        if (inputView == null) {
          val viewHolder = ExampleViewHolder()
          val inflater = LayoutInflater.from(this.context)
          val view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false) as ViewGroup
          inputView = view
          view.tag = viewHolder
          viewHolder
        } else {
          inputView.tag as ExampleViewHolder
        }

      viewHolder.button =
        ExampleButton(
          context = this.context,
          onShowLoginDialog = this.onShowLoginDialog,
          account = this.controller.profileAccountCurrent(),
          controller = this.controller)

      inputView.removeAllViews()
      inputView.addView(viewHolder.button)
      return inputView
    }
  }

  object ExampleButtons {
    var count = 0
  }

  class ExampleButton(
    context: Context,
    private val onShowLoginDialog: () -> (Unit),
    private val account: AccountType,
    private val controller: ProfilesControllerType) : Button(context) {

    private var subscription: ObservableSubscriptionType<AccountEvent>? = null
    private var clicked: Boolean = false
    private val count: Int = ExampleButtons.count++
    private val logger = LoggerFactory.getLogger(ExampleButton::class.java)

    init {
      this.text = "Example ${this.count}"
      this.setOnClickListener { this.onClick() }
    }

    override fun onDetachedFromWindow() {
      super.onDetachedFromWindow()
      this.logger.debug("[{}]: onDetachedFromWindow", this.count)
      this.clicked = false
      this.subscription?.unsubscribe()
    }

    override fun onAttachedToWindow() {
      super.onAttachedToWindow()
      this.logger.debug("[{}]: onAttachedToWindow", this.count)
    }

    private fun onClick() {
      this.logger.debug("[{}]: onClick", this.count)
      this.clicked = true
      this.subscription =
        this.controller.accountEvents()
          .subscribe { event -> this.onAccountEvent(event) }

      val loginState = this.account.loginState()
      if (account.requiresCredentials() && loginState.credentials == null) {
        this.onShowLoginDialog.invoke()
      } else {
        this.tryBorrow()
      }
    }

    private fun tryBorrow() {
      this.logger.debug("tryBorrow!")
    }

    private fun onAccountEvent(event: AccountEvent) {
      if (event is AccountEventLoginStateChanged) {
        if (event.accountID == this.account.id()) {
          return this.configureForState(event.state)
        }
      }
    }

    private fun configureForState(state: AccountLoginState) {
      UIThread.runOnUIThread {
        when (state) {
          AccountLoginState.AccountNotLoggedIn -> {
            this.clicked = false
          }
          AccountLoginState.AccountLoggingIn -> {
            this.clicked = false
          }
          is AccountLoginState.AccountLoginFailed -> {
            this.clicked = false
          }
          is AccountLoginState.AccountLoggedIn -> {
            this.clicked = false
            this.tryBorrow()
          }
          AccountLoginState.AccountLoggingOut -> {
            this.clicked = false
          }
          is AccountLoginState.AccountLogoutFailed -> {
            this.clicked = false
          }
        }
      }
    }
  }

  private fun getAccountProviders(): AccountProviderCollection {
    return this.accountProviders
  }
}
