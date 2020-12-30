import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Generate the README.md table.
 */

public final class ReadMe
{
  private ReadMe()
  {

  }

  public static void main(final String[] args)
    throws IOException
  {
    final var path = Paths.get("");
    try (var stream = Files.walk(path, 1)) {
      final var directories =
        stream.filter(p -> Files.isDirectory(p))
          .sorted()
          .collect(Collectors.toList());

      System.out.println("|Module|Description|");
      System.out.println("|------|-----------|");

      for (final var directory : directories) {
        final var moduleDirectory = directory.getFileName().toString();
        if (moduleDirectory.isEmpty()) {
          continue;
        }

        final var propertyFile = directory.resolve("gradle.properties");
        if (!Files.isRegularFile(propertyFile)) {
          continue;
        }

        final var properties = new Properties();
        try (final var input = Files.newInputStream(propertyFile)) {
          properties.load(input);
        }

        final var moduleName = properties.getProperty("POM_ARTIFACT_ID");
        final var rawDescription = properties.getProperty("POM_DESCRIPTION");
        var trimmed = rawDescription;
        trimmed = trimmed.replace("Library Simplified (", "");
        trimmed = trimmed.replace(")", "");
        System.out.printf("|[%s](%s)|%s|\n", moduleName, moduleDirectory, trimmed);
      }
    }
  }
}
