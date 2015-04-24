package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderTestActivity extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.reader);

    final TextView in_progress_text =
      NullCheck.notNull((TextView) this
        .findViewById(R.id.reader_position_text));
    final ProgressBar in_progress_bar =
      NullCheck.notNull((ProgressBar) this
        .findViewById(R.id.reader_position_progress));
    final ProgressBar in_loading =
      NullCheck.notNull((ProgressBar) this.findViewById(R.id.reader_loading));
    final WebView in_webview =
      NullCheck.notNull((WebView) this.findViewById(R.id.reader_webview));

    in_loading.setVisibility(View.INVISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_webview.setVisibility(View.VISIBLE);
    in_webview.setLongClickable(false);
    in_webview.setOnLongClickListener(new OnLongClickListener() {
      @Override public boolean onLongClick(
        final @Nullable View v)
      {
        Log.d("LONG", "STOP THIS BULLSHIT");
        return true;
      }
    });
    in_webview.loadUrl("file:///storage/sdcard0/index.html");
  }
}
