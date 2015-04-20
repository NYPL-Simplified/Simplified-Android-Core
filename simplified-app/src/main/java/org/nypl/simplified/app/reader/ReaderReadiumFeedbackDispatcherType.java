package org.nypl.simplified.app.reader;

import java.net.URI;

public interface ReaderReadiumFeedbackDispatcherType
{
  void dispatch(
    URI uri,
    ReaderReadiumFeedbackListenerType l);
}
