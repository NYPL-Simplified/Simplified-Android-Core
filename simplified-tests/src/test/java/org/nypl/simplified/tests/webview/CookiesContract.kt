package org.nypl.simplified.tests.webview

import android.webkit.CookieManager
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.webview.WebViewUtilities
import java.io.File

abstract class CookiesContract {

  @Test
  fun testDumpCookiesV9() {
    val webViewDataDir = CookiesContract::class.java.getResource("app_webview_v9")!!.file

    val accountCookies = WebViewUtilities.dumpCookiesAsAccountCookies(
      CookieManager.getInstance(),
      File(webViewDataDir)
    )

    Assertions.assertArrayEquals(
      arrayOf(
        AccountCookie("https://shibboleth.columbia.edu", "JSESSIONID=2BD741F198E14A5D9EDA9C8B54611EE2; Domain=shibboleth.columbia.edu; Path=/idp/; Secure; HttpOnly"),
        AccountCookie("http://cas.columbia.edu", "cuPrivacyNotice=1; Domain=cas.columbia.edu; Path=/; Expires=Thu, 31 Dec 2020 03:41:10 GMT"),
        AccountCookie("https://cas.columbia.edu", "JSESSIONID=D3C7272076CD4B75ADD6E8E83C8EE48B; Domain=cas.columbia.edu; Path=/cas; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "TGC=DA7693447E074DFDBA14E3C68626534B; Domain=cas.columbia.edu; Path=/cas/; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "BIGipServer~CUIT~cas.columbia.edu-443-pool=8D9722CD54194479B185ED0ECED79C7C; Domain=cas.columbia.edu; Path=/; Expires=Tue, 01 Dec 2020 03:46:18 GMT; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "BIGipServer~CUIT~shibboleth.columbia.edu-128.59.105.181.443-pool=AC54F46FC6DC4ACBA40DE0BD16D02C6D; Domain=shibboleth.columbia.edu; Path=/; Expires=Tue, 01 Dec 2020 03:56:20 GMT; Secure; HttpOnly"),
        AccountCookie("https://strict.com", "foo2=bar2; Domain=strict.com; Path=/; Secure; SameSite=Strict"),
        AccountCookie("https://lax.com", "foo3=bar3; Domain=lax.com; Path=/; Secure; SameSite=Lax"),
      ),
      accountCookies.toTypedArray()
    )
  }

  @Test
  fun testDumpCookiesV10() {
    val webViewDataDir = CookiesContract::class.java.getResource("app_webview_v10")!!.file

    val accountCookies = WebViewUtilities.dumpCookiesAsAccountCookies(
      CookieManager.getInstance(),
      File(webViewDataDir)
    )

    Assertions.assertArrayEquals(
      arrayOf(
        AccountCookie("https://cas.columbia.edu", "BIGipServer~CUIT~cas.columbia.edu-443-pool=F0F1D7969FA64A69BE4C1348067C4029; Domain=cas.columbia.edu; Path=/; Expires=Sun, 22 Nov 2020 22:00:53 GMT; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "BIGipServer~CUIT~shibboleth.columbia.edu-128.59.105.181.443-pool=B845696DA38D4663A305B1134005A291; Domain=shibboleth.columbia.edu; Path=/; Expires=Sun, 22 Nov 2020 22:10:53 GMT; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "JSESSIONID=528A0FAA87464369BDCA20E2446E8914; Domain=cas.columbia.edu; Path=/cas; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "JSESSIONID=4317D3A99F1642FE9EDE686195AD4303; Domain=shibboleth.columbia.edu; Path=/idp/; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "TGC=6376CFCC2DDA4C22B45A79CC8D8980E2; Domain=cas.columbia.edu; Path=/cas/; Secure; HttpOnly"),
        AccountCookie("http://cas.columbia.edu", "cuPrivacyNotice=1; Domain=cas.columbia.edu; Path=/; Expires=Tue, 22 Dec 2020 21:55:45 GMT"),
        AccountCookie("http://strict.com", "foo1=bar1; Domain=strict.com; Path=/; SameSite=Strict"),
        AccountCookie("http://lax.com", "foo2=bar2; Domain=lax.com; Path=/; SameSite=Lax"),
        AccountCookie("http://none.com", "foo3=bar3; Domain=none.com; Path=/"),
      ),
      accountCookies.toTypedArray()
    )
  }

  @Test
  fun testDumpCookiesV11() {
    val webViewDataDir = CookiesContract::class.java.getResource("app_webview_v11")!!.file

    val accountCookies = WebViewUtilities.dumpCookiesAsAccountCookies(
      CookieManager.getInstance(),
      File(webViewDataDir)
    )

    Assertions.assertArrayEquals(
      arrayOf(
        AccountCookie("https://secure.com", "foo1=bar1; Domain=secure.com; Path=/; Secure"),
        AccountCookie("http://none.com", "foo4=bar4; Domain=none.com; Path=/; SameSite=None"),
        AccountCookie("https://cas.columbia.edu", "BIGipServer~CUIT~cas.columbia.edu-443-pool=8B473523635E474181816476172649DF; Domain=cas.columbia.edu; Path=/; Expires=Tue, 01 Dec 2020 02:20:24 GMT; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "JSESSIONID=9DA78F283D0A4F43B2199FCC882D5733; Domain=cas.columbia.edu; Path=/cas; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "JSESSIONID=149145B41E7F472987EB9AE4B0FA055E; Domain=shibboleth.columbia.edu; Path=/idp/; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "TGC=A8246D532622450A8C5DE64E2139C690; Domain=cas.columbia.edu; Path=/cas/; Secure; HttpOnly"),
        AccountCookie("http://cas.columbia.edu", "cuPrivacyNotice=1; Domain=cas.columbia.edu; Path=/; Expires=Thu, 31 Dec 2020 02:15:16 GMT"),
        AccountCookie("https://shibboleth.columbia.edu", "BIGipServer~CUIT~shibboleth.columbia.edu-128.59.105.181.443-pool=8D147D1056C84AC09AA3004AB2748591; Domain=shibboleth.columbia.edu; Path=/; Expires=Tue, 01 Dec 2020 02:30:25 GMT; Secure; HttpOnly"),
      ),
      accountCookies.toTypedArray()
    )
  }

  @Test
  fun testDumpCookiesV12() {
    val webViewDataDir = CookiesContract::class.java.getResource("app_webview_v12")!!.file

    val accountCookies = WebViewUtilities.dumpCookiesAsAccountCookies(
      CookieManager.getInstance(),
      File(webViewDataDir)
    )

    Assertions.assertArrayEquals(
      arrayOf(
        AccountCookie("https://cas.columbia.edu", "BIGipServer~CUIT~cas.columbia.edu-443-pool=17567F5540E1410B9CA31919BF4643CA; Domain=cas.columbia.edu; Path=/; Expires=Sun, 22 Nov 2020 22:12:05 GMT; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "BIGipServer~CUIT~shibboleth.columbia.edu-128.59.105.181.443-pool=376C08BC24AD40B8B16BDA6DB6A866CB; Domain=shibboleth.columbia.edu; Path=/; Expires=Sun, 22 Nov 2020 22:22:07 GMT; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "JSESSIONID=0F7CC570F48142A3A49DDE156E8DD256; Domain=cas.columbia.edu; Path=/cas; Secure; HttpOnly"),
        AccountCookie("https://shibboleth.columbia.edu", "JSESSIONID=F13A07C875EC4DBF98B3248A61D80DB2; Domain=shibboleth.columbia.edu; Path=/idp/; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "TGC=411D7A3C6ADD43C6980D78137BA60A32; Domain=cas.columbia.edu; Path=/cas/; Secure; HttpOnly"),
        AccountCookie("https://cas.columbia.edu", "cuPrivacyNotice=1; Domain=cas.columbia.edu; Path=/; Expires=Tue, 22 Dec 2020 22:06:54 GMT"),
        AccountCookie("https://strict.com", "foo1=bar1; Domain=strict.com; Path=/; SameSite=Strict"),
        AccountCookie("https://lax.com", "foo2=bar2; Domain=lax.com; Path=/; SameSite=Lax"),
        AccountCookie("https://none.com", "foo3=bar3; Domain=none.com; Path=/; SameSite=None"),
      ),
      accountCookies.toTypedArray()
    )
  }
}
