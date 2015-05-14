package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;

public interface ReaderTOCViewSelectionListenerType
{
  void onTOCBackSelected();

  void onTOCItemSelected(
    TOCElement e);
}
