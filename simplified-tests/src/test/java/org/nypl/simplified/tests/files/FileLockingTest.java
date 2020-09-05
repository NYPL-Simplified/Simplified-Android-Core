package org.nypl.simplified.tests.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileLockingTest extends FileLockingContract {

  @Override
  protected Logger logger() {
    return LoggerFactory.getLogger(FileLockingTest.class);
  }

}
