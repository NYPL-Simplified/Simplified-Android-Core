package org.nypl.simplified.ui.catalog

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.net.URI

class CatalogFeedWebFragment : Fragment(R.layout.catalog_feed_web) {

  companion object {
    internal const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFeedWebFragment.parameters"

    fun create(parameters: CatalogFeedArguments): CatalogFeedWebFragment {
        val fragment = CatalogFeedWebFragment()
        fragment.arguments = bundleOf(PARAMETERS_ID to parameters)
        return fragment
    }
  }

  private val parameters: CatalogFeedArguments by lazy {
    requireArguments()[PARAMETERS_ID] as CatalogFeedArguments
  }

  private val services: ServiceDirectoryType = Services.serviceDirectory()

  private val profilesController = services.requireService(ProfilesControllerType::class.java)
  private val feedLoader = this.services.requireService(FeedLoaderType::class.java)

  private val logger = LoggerFactory.getLogger(CatalogFeedWebFragment::class.java)

  private lateinit var webView: WebView

  private val listener: FragmentListenerType<CatalogFeedEvent> by fragmentListeners()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    webView = view.findViewById(R.id.feedWebView)
    webView.settings.javaScriptEnabled = true

    webView.webChromeClient = object : WebChromeClient() {
      override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        logger.debug("JS Console: $message")
        super.onConsoleMessage(message, lineNumber, sourceID)
      }
    }
    webView.addJavascriptInterface(CatalogWebInterface { loadBookDetail(it) }, "AndroidCatalog")

    webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
        view?.evaluateJavascript(javascriptToInject(), null)
      }

      // override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
      //   logger.error("override url: webresourcerequest")
      //   return request?.let {
      //     handleBookDetailUrl(it.url)
      //   } ?: false
      // }
      //
      // override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
      //   logger.error("override url: string")
      //   val uri = Uri.parse(url)
      //   return handleBookDetailUrl(uri)
      // }
      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
      ): WebResourceResponse? {
        return super.shouldInterceptRequest(view, request)
      }

      // override fun shouldInterceptRequest(
      //   view: WebView?,
      //   request: WebResourceRequest?
      // ): WebResourceResponse? {
      //   logger.error("override shouldInterceptRequest (request): ${request?.url}")
      //   if (request?.url?.host?.contains("circulation.openebooks.us") == true &&
      //     request.url?.path?.contains("USOEI/works") == true
      //   ) {
      //     logger.error("Path contains works: ${request.url}")
      //     this@CatalogFeedWebFragment.lifecycleScope.launch(Dispatchers.Main.immediate) {
      //       webView.stopLoading()
      //       loadBookDetail("https://circulation.openebooks.us/USOEI/works/Axis%20360%20ID/0017105971")
      //     }
      //     //grab URL from request path
      //   }
      //   return super.shouldInterceptRequest(view, request)
      // }

      override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
      ) {
        logger.error("Received error from $failingUrl" +
          " with code $errorCode" +
          " and description: $description")
      }

      @RequiresApi(Build.VERSION_CODES.M)
      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
      ) {
        logger.error("Received error from ${request?.url}" +
          " with code ${error?.errorCode}" +
          " and description ${error?.description}")
      }

      // fun handleBookDetailUrl(uri: Uri): Boolean {
      //   return if (uri.path?.contains("book") == true) {
      //     logger.error("Path contains book")
      //     uri.path?.let { path ->
      //       val bookUrl = path.split("/").last()
      //       logger.error("Loading book detail")
      //       loadBookDetail(bookUrl)
      //     }
      //     true
      //   } else {
      //     logger.error("Webview handling request normally")
      //     false
      //   }
      // }
    }

    logger.error("ParamsURI: ${(parameters as CatalogFeedArguments.CatalogFeedArgumentsRemote).feedURI}")

    val testUrl = "https://simplye-web-git-oe-326-mobile-webview-nypl.vercel.app"

    val initialFeedUri = (parameters as CatalogFeedArguments.CatalogFeedArgumentsRemote).feedURI
    val slug = with(initialFeedUri.toString()) {
      when {
        contains("circulation.openebooks") -> "oe-qa"
        contains("simply") -> "simply-qa"
        else -> this
      }
    }

    val cookie = buildAuthCookie(slug)
    logger.error("Writing cookie: $cookie")
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().setCookie(
      testUrl,
      cookie
    )

    webView.loadUrl("$testUrl/$slug")
  }

  private fun buildAuthCookie(slug: String): String {
    val currentCreds = profilesController.profileCurrent().mostRecentAccount().loginState.credentials
    val creds = when (currentCreds) {
      is AccountAuthenticationCredentials.Basic -> {
        val username = currentCreds.userName.value
        val password = currentCreds.password.value
        "$username:$password"
      }
      is AccountAuthenticationCredentials.OAuthWithIntermediary -> TODO()
      is AccountAuthenticationCredentials.SAML2_0 -> TODO()
      null -> TODO()
    }

    val encodedCreds = Base64.encodeToString(creds.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val cookieValue =
      "{\"token\": \"Basic ${encodedCreds}\", \"methodType\": \"http://opds-spec.org/auth/basic\"}"
    val cookiePrefix = "CPW_AUTH_COOKIE/$slug"
    val urlEscapedPrefix = Uri.encode(cookiePrefix)
    val cookie = "$urlEscapedPrefix=$cookieValue"
    return cookie
  }

  fun loadBookDetail(bookUrl: String) {
    val currentAccount = profilesController.profileCurrent().mostRecentAccount()
    val currentAccountID = currentAccount.id
    val loginState = currentAccount.loginState
    val authentication =
      AccountAuthenticatedHTTP.createAuthorizationIfPresent(loginState.credentials)

    val future = feedLoader.fetchURI(
      currentAccountID,
      URI.create(bookUrl),
      authentication,
      "GET"
    )
    future.map {
      when (it) {
        is FeedLoaderResult.FeedLoaderSuccess -> {
          logger.error("FeedLoaderSuccess: ${it.feed}")
          val event = CatalogFeedEvent.OpenBookDetail(
            CatalogFeedArguments.CatalogFeedArgumentsRemote(
              "title", //title of the lane??
              CatalogFeedOwnership.OwnedByAccount(currentAccountID),
              URI.create(bookUrl), //feed URI?
              isSearchResults = true //guess this would be whether detail was entered from a search results?
            ),
            //We'll need to handle With/Without Groups and EntryOPDS/Corrupt types
            ((it.feed as Feed.FeedWithoutGroups).entriesInOrder[0] as FeedEntry.FeedEntryOPDS)
          )

          postEvent(event)
        }
        is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication -> {
          logger.error("FeedLoaderFailure: FeedLoaderFailedAuthentication")
        }
        is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral -> {
          logger.error("FeedLoaderFailure: FeedLoaderFailedGeneral")
        }
      }
    }
  }

  private fun postEvent(event: CatalogFeedEvent) {
    lifecycleScope.launch {
      listener.post(event)
    }
  }

  private fun javascriptToInject() : String = "(function (document) {\n" +
    "    function handleLinkClick(evt) {\n" +
    "        // determine if it's a book\n" +
    "        var url = new URL(evt.currentTarget.href);\n" +
    "        var isBookLink = url.pathname.startsWith(\"/oe-qa/book/https\");\n" +
    "        if (isBookLink){\n" +
    "            // don't navigate\n" +
    "            evt.preventDefault();\n" +
    "            var encodedUri = url.pathname.split(\"/\")[3];\n" +
    "            var uri = decodeURIComponent(encodedUri);\n" +
    "            console.log(\"BOOK CLICKED\", uri)\n" +
    "            AndroidCatalog.openBookDetail(uri)\n" +
    "        }\n" +
    "    }\n" +
    "    var links = document.querySelectorAll(\"a\");\n" +
    "    for (var i = 0; i < links.length; i++) {\n" +
    "        links[i].addEventListener(\"click\", handleLinkClick);\n" +
    "    }\n" +
    "})(document);"
}

class CatalogWebInterface(private val onViewBook: (String) -> Unit) {
  @JavascriptInterface
  fun openBookDetail(bookUrl: String) = onViewBook(bookUrl)
}
