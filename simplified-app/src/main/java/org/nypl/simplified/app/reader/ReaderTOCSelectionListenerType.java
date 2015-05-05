package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;

public interface ReaderTOCSelectionListenerType
{
  void onTOCSelectionReceived(
    TOCElement e);
}
