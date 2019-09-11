package org.nypl.simplified.app.catalog

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.utilities.FadeUtilities
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.stack.ImmutableStack
import org.slf4j.LoggerFactory

/**
 * An abstract activity providing *up* navigation using the home button.
 */

abstract class CatalogActivity : NavigationDrawerActivity() {

  private val logger = LoggerFactory.getLogger(CatalogActivity::class.java)

  protected lateinit var upStack: ImmutableStack<CatalogFeedArguments>

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    /*
     * Find the up stack, or create an empty one.
     */

    this.upStack = run {
      val intent = this.intent
      if (intent != null) {
        val bundle = intent.extras
        if (bundle != null) {
          val stack =
            bundle.getSerializable(CATALOG_UP_STACK_ID) as ImmutableStack<CatalogFeedArguments>?
          if (stack != null) {
            stack
          } else {
            ImmutableStack.empty<CatalogFeedArguments>()
          }
        } else {
          ImmutableStack.empty<CatalogFeedArguments>()
        }
      } else {
        ImmutableStack.empty<CatalogFeedArguments>()
      }
    }
  }

  override fun onStart() {
    super.onStart()

    val bar = this.getSupportActionBar()
    if (bar != null) {
      if (!this.upStack.isEmpty) {
        this.logger.debug("replacing navigation drawer indicator with up arrow")
        bar.displayOptions =
          (ActionBar.DISPLAY_SHOW_TITLE
            or ActionBar.DISPLAY_HOME_AS_UP
            or ActionBar.DISPLAY_SHOW_HOME)
      }
    }
  }

  /**
   * If the user presses the "home" button (configured to be the "up" button for non-empty
   * stacks), pop the up stack and start a new activity. If the new stack is empty, start a new
   * activity at the root. Note that this is the *correct* hierarchical traversal behaviour:
   * The _up_ button is supposed to move up the feed _hierarchically_, whilst the _back_ button
   * is supposed to move through the feed _historically_. This method gives the correct _back_
   * button behaviour by putting the popped feed onto the back stack (and removing it from the
   * up stack).
   */

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      android.R.id.home -> {
        if (!this.upStack.isEmpty) {
          this.logger.debug("popping up stack")
          val popped = this.upStack.pop()
          catalogActivityForkNew(popped.left)
          true
        } else {
          super.onOptionsItemSelected(menuItem)
        }
      }
      else -> {
        super.onOptionsItemSelected(menuItem)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val contentArea = this.contentFrame
    FadeUtilities.fadeIn(contentArea, FadeUtilities.DEFAULT_FADE_DURATION)
  }

  /**
   * Decide which class is necessary to show a feed based on the given
   * arguments.
   *
   * @param args The arguments
   *
   * @return An activity class
   */

  private fun getFeedClassForArguments(args: CatalogFeedArguments): Class<out CatalogFeedActivity> {
    return when (args) {
      is CatalogFeedArguments.CatalogFeedArgumentsRemote ->
        MainCatalogActivity::class.java
      is CatalogFeedArguments.CatalogFeedArgumentsLocalBooks ->
        when (args.selection) {
          FeedBooksSelection.BOOKS_FEED_LOANED -> MainBooksActivity::class.java
          FeedBooksSelection.BOOKS_FEED_HOLDS -> MainHoldsActivity::class.java
        }
    }
  }

  protected fun catalogActivityForkNew(args: CatalogFeedArguments) {
    val b = Bundle()
    CatalogFeedActivity.setActivityArguments(b, args)
    val i = Intent(this, this.getFeedClassForArguments(args))
    i.putExtras(b)
    i.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    this.startActivity(i)
  }

  protected fun catalogActivityForkNewReplacing(args: CatalogFeedArguments) {
    this.catalogActivityForkNew(args)
    this.finish()
    this.overridePendingTransition(0, 0)
  }

  companion object {
    private const val CATALOG_UP_STACK_ID: String =
      "org.nypl.simplified.app.CatalogActivity.up_stack"

    /**
     * Set the arguments for the activity that will be created.
     *
     * @param b The argument bundle
     * @param upStack The up stack for the created activity
     */

    fun setActivityArguments(
      b: Bundle,
      upStack: ImmutableStack<CatalogFeedArguments>
    ) {
      b.putSerializable(CATALOG_UP_STACK_ID, upStack)
    }
  }
}
