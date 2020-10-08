package org.nypl.simplified.tests.files;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;

import java.io.File;

public abstract class FilesContract {

  @Test
  public final void testDeleteRecreate()
      throws Exception
  {
    final File tmp = DirectoryUtilities.directoryCreateTemporary();
    final File file0 = new File(tmp, "file.txt");

    FileUtilities.fileWriteUTF8(file0, "Hello.");
    Assert.assertTrue(file0 + " is file", file0.isFile());
    FileUtilities.fileDelete(file0);
    Assert.assertFalse(file0 + " does not exist", file0.exists());
    FileUtilities.fileWriteUTF8(file0, "Hello.");
    Assert.assertTrue(file0 + " is file", file0.isFile());
    FileUtilities.fileDelete(file0);
    Assert.assertFalse(file0 + " does not exist", file0.exists());
  }

}
