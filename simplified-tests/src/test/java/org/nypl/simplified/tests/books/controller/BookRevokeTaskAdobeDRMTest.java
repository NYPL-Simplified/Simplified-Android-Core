package org.nypl.simplified.tests.books.controller;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BookRevokeTaskAdobeDRMTest extends BookRevokeTaskAdobeDRMContract {

  @NotNull
  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(BookRevokeTaskAdobeDRMTest.class);
  }
}
