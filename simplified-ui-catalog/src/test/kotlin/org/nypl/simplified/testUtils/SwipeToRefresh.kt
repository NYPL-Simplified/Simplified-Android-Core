package org.nypl.simplified.testUtils

import android.view.View
import android.view.animation.Animation
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import io.mockk.mockk
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.Description
import org.hamcrest.Matcher
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/*
https://github.com/robolectric/robolectric/issues/5375

Hacky way to force the onRefreshListener for a SwipeRefreshLayout to be invoked when running
a fragment scenario test in the JVM using Robolectric

 */

fun robolectricSwipeToRefresh(): ViewAction {
  return object : ViewAction {
    override fun getConstraints(): Matcher<View>? {
      return object : BaseMatcher<View>() {
        override fun matches(item: Any): Boolean {
          return isA(SwipeRefreshLayout::class.java).matches(item)
        }

        override fun describeMismatch(item: Any, mismatchDescription: Description) {
          mismatchDescription.appendText(
            "Expected SwipeRefreshLayout or its Descendant, but got other View"
          )
        }

        override fun describeTo(description: Description) {
          description.appendText(
            "Action SwipeToRefresh to view SwipeRefreshLayout or its descendant"
          )
        }
      }
    }

    override fun getDescription(): String {
      return "Perform swipeToRefresh on the SwipeRefreshLayout"
    }

    override fun perform(uiController: UiController, view: View) {
      val swipeRefreshLayout = view as SwipeRefreshLayout
      swipeRefreshLayout.run {
        isRefreshing = true
        // set mNotify to true
        val notify = SwipeRefreshLayout::class.memberProperties.find {
          it.name == "mNotify"
        }
        notify?.isAccessible = true
        if (notify is KMutableProperty<*>) {
          notify.setter.call(this, true)
        }
        // mockk mRefreshListener onAnimationEnd
        val refreshListener = SwipeRefreshLayout::class.memberProperties.find {
          it.name == "mRefreshListener"
        }
        refreshListener?.isAccessible = true
        val animatorListener = refreshListener?.get(this) as Animation.AnimationListener
        animatorListener.onAnimationEnd(mockk())
      }
    }
  }
}
