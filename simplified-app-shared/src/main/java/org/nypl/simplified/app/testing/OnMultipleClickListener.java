package org.nypl.simplified.app.testing;

import android.preference.Preference;
import android.view.View;


public abstract class OnMultipleClickListener implements Preference.OnPreferenceClickListener {
  private static final String TAG = OnMultipleClickListener.class.getSimpleName();

  private static final long MIN_DELAY_MS = 1000;

  private long last_click_time;

  private int clicks = 0;


  @Override
  public boolean onPreferenceClick(final Preference preference) {
    final long click_time = this.last_click_time;
    final long now = System.currentTimeMillis();
    this.last_click_time = now;
    if (now - click_time < MIN_DELAY_MS) {
      // Too fast: ignore
      this.clicks += 1;

      if (this.clicks == 7)
      {
        this.clicks = 0;

        return this.onMultipleClick(preference);
      }


    }

    return false;
  }

  /**
   * Called when a view has been clicked.
   *
   * @param v The view that was clicked.
   */
  public abstract boolean onMultipleClick(Preference v);

  /**
   * Wraps an {@link View.OnClickListener} into an {@link OnMultipleClickListener}.<br/>
   * The argument's {@link View.OnClickListener#onClick(View)} method will be called when a single click is registered.
   *
   * @param onClickListener The listener to wrap.
   * @return the wrapped listener.
   */
  public static Preference.OnPreferenceClickListener wrap(final Preference.OnPreferenceClickListener onClickListener) {
    return new OnMultipleClickListener() {
      @Override
      public boolean onMultipleClick(Preference v) {
        return onClickListener.onPreferenceClick(v);
      }
    };
  }
}
