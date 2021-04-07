package org.nypl.simplified.tests.files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;

import java.io.File;

public final class FilesTest {

  @Test
  public final void testDeleteRecreate()
    throws Exception {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File file0 = new File(tmp, "file.txt");

    FileUtilities.fileWriteUTF8(file0, "Hello.");
    Assertions.assertTrue(file0.isFile(), file0 + " is file");
    FileUtilities.fileDelete(file0);
    Assertions.assertFalse(file0.exists(), file0 + " does not exist");
    FileUtilities.fileWriteUTF8(file0, "Hello.");
    Assertions.assertTrue(file0.isFile(), file0 + " is file");
    FileUtilities.fileDelete(file0);
    Assertions.assertFalse(file0.exists(), file0 + " does not exist");
  }

}
