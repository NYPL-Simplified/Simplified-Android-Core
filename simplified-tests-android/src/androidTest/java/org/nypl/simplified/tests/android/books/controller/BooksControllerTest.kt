package org.nypl.simplified.tests.android.books.controller

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4

import org.junit.Before
import org.junit.runner.RunWith
import org.nypl.simplified.tests.books.controller.BooksControllerContract

@RunWith(AndroidJUnit4::class)
@MediumTest
class BooksControllerTest : BooksControllerContract() {

  private var instrumentationContext: Context? = null

  private fun initContext(): Context
  {
    return if (this.instrumentationContext == null) {
      val context = InstrumentationRegistry.getContext()!!
      this.instrumentationContext = context
      context
    } else {
      this.instrumentationContext!!
    }
  }

  override fun context(): Context {
    return this.initContext()
  }
}
