package org.nypl.simplified.app.testing;

import android.preference.Preference;


/**
 *
 */
public abstract class OnMultipleClickListener implements Preference.OnPreferenceClickListener {
  private static final String TAG = OnMultipleClickListener.class.getSimpleName();

  private static final long MIN_DELAY_MS = 1000;

  private long last_click_time;

  private int clicks;


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
   * @return boolean
   */
  public abstract boolean onMultipleClick(Preference v);

  /**
   * Wraps an {View.OnClickListener} into an {@link OnMultipleClickListener}.<br/>
   * The argument's {View.OnClickListener#onClick(View)} method will be called when a single click is registered.
   *
   * @param listener The listener to wrap.
   * @return the wrapped listener.
   */
  public static Preference.OnPreferenceClickListener wrap(final Preference.OnPreferenceClickListener listener) {
    return new OnMultipleClickListener() {
      @Override
      public boolean onMultipleClick(final Preference v) {
        return listener.onPreferenceClick(v);
      }
    };
  }
}
