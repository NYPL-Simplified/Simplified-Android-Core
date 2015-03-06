package org.nypl.simplified.app;

import android.view.View;
import android.view.ViewPropertyAnimator;

public final class Fade
{
  public static final int DEFAULT_FADE_DURATION = 100;

  /**
   * Set the given view to <i>visible</i>, and fade in the view from zero to
   * maximum alpha over <i>duration</i>.
   *
   * @param v
   *          The view
   * @param duration
   *          The duration
   */

  public static void fadeIn(
    final View v,
    final int duration)
  {
    v.setVisibility(View.VISIBLE);
    v.setAlpha(0.0f);
    final ViewPropertyAnimator a = v.animate();
    a.setDuration(duration);
    a.alpha(1.0f);
  }
}
