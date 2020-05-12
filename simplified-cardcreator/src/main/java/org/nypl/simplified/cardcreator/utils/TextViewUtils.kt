package org.nypl.simplified.cardcreator.utils

import android.annotation.SuppressLint
import android.os.Handler
import android.widget.TextView

/**
 * TextView utilities
 */

/**
 * Start ellipses animation
 */
fun TextView.startEllipses() {
  val ellipses = this
  val handler = Handler()
  val runnable: Runnable = object : Runnable {
    var count = 0

    @SuppressLint("SetTextI18n")
    override fun run() {
      count++
      when (count) {
        1 -> {
          ellipses.text = ""
        }
        2 -> {
          ellipses.text = "."
        }
        3 -> {
          ellipses.text = ".."
        }
        4 -> {
          ellipses.text = "..."
        }
      }
      if (count == 4) count = 0
      handler.postDelayed(this, 2 * 300)
    }
  }
  handler.postDelayed(runnable, 1 * 300)
}
