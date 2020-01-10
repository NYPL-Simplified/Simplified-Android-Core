package org.nypl.simplified.tests.sandbox

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.app.AppCompatActivity

class LaneActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.feed_lane)

    val lane = this.findViewById<LinearLayout>(R.id.feedLaneCovers)
    lane.removeAllViews()

    val endSpaceLayoutParams =
      LinearLayout.LayoutParams(
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversEndSpace),
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

    val spaceLayoutParams =
      LinearLayout.LayoutParams(
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversSpace),
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

    val coverLayoutParams =
      LinearLayout.LayoutParams(
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversWidth),
        this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

    run {
      val space = Space(this)
      space.layoutParams = endSpaceLayoutParams
      lane.addView(space)
    }

    for (i in 0..32) {
      if (i > 0) {
        val space = Space(this)
        space.layoutParams = spaceLayoutParams
        lane.addView(space)
      }

      val imageView = ImageView(this)
      imageView.scaleType = ImageView.ScaleType.FIT_XY
      imageView.setImageResource(R.drawable.cover)
      imageView.layoutParams = coverLayoutParams
      imageView.setColorFilter(this.randomColor(), android.graphics.PorterDuff.Mode.MULTIPLY)

      lane.addView(imageView)
    }

    run {
      val space = Space(this)
      space.layoutParams = endSpaceLayoutParams
      lane.addView(space)
    }
  }

  private fun randomColor(): Int {
    val r = 128.0 + (Math.random() * 128.0)
    val g = 128.0 + (Math.random() * 128.0)
    val b = 128.0 + (Math.random() * 128.0)
    return Color.rgb(r.toInt(), g.toInt(), b.toInt())
  }
}
