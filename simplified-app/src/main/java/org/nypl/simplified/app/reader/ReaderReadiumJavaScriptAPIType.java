package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.OptionType;

/**
 * The type of the JavaScript API exposed by Readium.
 */

public interface ReaderReadiumJavaScriptAPIType
{
  void openBook(
    org.readium.sdk.android.Package p,
    ReaderViewerSettings vs,
    OptionType<ReaderOpenPageRequest> r);
}
