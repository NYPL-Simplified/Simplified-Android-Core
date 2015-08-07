package org.nypl.simplified.app.utilities;

import android.view.View;
import android.view.ViewPropertyAnimator;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Utilities for animating fades on views.
 */

public final class FadeUtilities
{
  /**
   * The default fade duration.
   */

  public static final int DEFAULT_FADE_DURATION = 100;

  private FadeUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Set the given view to <i>visible</i>, and fade in the view from zero to
   * maximum alpha over <i>duration</i>.
   *
   * @param v        The view
   * @param duration The duration
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

  /**
   * Set the given view to <i>invisible</i> after fading out the view to zero
   * from maximum alpha over <i>duration</i>.
   *
   * @param v        The view
   * @param duration The duration
   */

  public static void fadeOut(
    final View v,
    final int duration)
  {
    v.setAlpha(1.0f);
    final ViewPropertyAnimator a = v.animate();
    a.setDuration(duration);
    a.alpha(0.0f);
    a.withEndAction(
      new Runnable()
      {
        @Override public void run()
        {
          v.setVisibility(View.INVISIBLE);
        }
      });
  }
}
