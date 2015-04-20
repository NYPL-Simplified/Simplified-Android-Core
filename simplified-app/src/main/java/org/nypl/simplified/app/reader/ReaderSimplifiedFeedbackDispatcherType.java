package org.nypl.simplified.app.reader;

import java.net.URI;

public interface ReaderSimplifiedFeedbackDispatcherType
{
  void dispatch(
    URI uri,
    ReaderSimplifiedFeedbackListenerType l);
}
