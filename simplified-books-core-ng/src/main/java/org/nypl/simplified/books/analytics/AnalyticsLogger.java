package org.nypl.simplified.books.analytics;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.logging.LogUtilities;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Skullbonez on 3/11/2018.
 *
 * This logger is to be AS FAILSAFE AS POSSIBLE.
 * Silent failures are allowed here.  We want this
 * to be a "best effort" logger - it is not to
 * crash the app!
 *
 */

public class AnalyticsLogger {

  private static final Logger LOG = LogUtilities.getLog(AnalyticsLogger.class);

  private final String analytics_server_uri = "http://ec2-18-217-127-216.us-east-2.compute.amazonaws.com:8080/upload.log";
  private final String log_file_name = "analytics_log.txt";
  private final int log_file_size_limit =  1024 * 1024 * 10;
  private final int log_file_push_limit = 1024 * 2;
  private BufferedWriter analytics_output = null;
  private File directory_analytics = null;
  private boolean log_size_limit_reached = false;
  private AtomicBoolean is_logging_paused = new AtomicBoolean(false);

  private AnalyticsLogger(
      File in_directory_analytics)
  {
    directory_analytics = NullCheck.notNull(in_directory_analytics, "analytics");
    init();
  }

  public static AnalyticsLogger create(
      final File directory_analytics)
  {
    return new AnalyticsLogger(directory_analytics);
  }

  private void init() {
    if ( log_size_limit_reached ) {
      // Don't bother trying to re-init if the log is full.
      return;
    }
    try {
      File log_file = new File(directory_analytics, log_file_name);
      // Stop logging after 10MB (future releases will transmit then delete this file)
      if (log_file.length() < log_file_size_limit) {
        FileWriter logWriter = new FileWriter(log_file, true);
        analytics_output = new BufferedWriter(logWriter);
      } else {
        log_size_limit_reached = true;
      }
    } catch (Exception e) {
      LOG.debug("Ignoring exception: init raised: ", e);
    }
  }

  private String readLogFile(String file) throws IOException {
    BufferedReader reader;
    reader = new BufferedReader(new FileReader(file));
    String line = null;
    StringBuilder stringBuilder = new StringBuilder();
    String ls = System.getProperty("line.separator");

    try {
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append(ls);
      }

      return stringBuilder.toString();
    } catch ( Exception e ) {
        LOG.debug("Ignoring exception: readLogFile raised: ", e);
    } finally {
      reader.close();
    }

    return "";
  }

  private void clearLogFile(String file) throws FileNotFoundException {
    PrintWriter writer = new PrintWriter(file);
    writer.print("");
    writer.close();
  }

  public void writeToAnalyticsServer(String deviceId) {

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        is_logging_paused.set(true);

        final HTTPType http = HTTP.newHTTP();
        final OptionType<HTTPAuthType> auth =
            Option.some((HTTPAuthType) HTTPAuthBasic.create(deviceId, ".S23gLhfW/n:#CPD"));

        final HTTPResultType<InputStream> result;

        try {
          String logFile = readLogFile(directory_analytics + "/" + log_file_name);
          if ( logFile.length() > 0 ) {
            result = http.post(auth, new URI(analytics_server_uri), logFile.getBytes(), "application/json");
            result.matchResult(

                new HTTPResultMatcherType<InputStream, Unit, Exception>() {
                  @Override
                  public Unit onHTTPError(final HTTPResultError<InputStream> error) throws Exception {
                    return Unit.unit();
                  }

                  @Override
                  public Unit onHTTPException(final HTTPResultException<InputStream> exception) throws Exception {
                    return Unit.unit();
                  }

                  @Override
                  public Unit onHTTPOK(final HTTPResultOKType<InputStream> result) throws Exception {
                    // Clear the log file.  Start logging from scratch.
                    clearLogFile(directory_analytics + "/" + log_file_name);
                    return Unit.unit();
                  }
                }
            );
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          is_logging_paused.set(false);
        }
      }
    });

    t.start();
  }

  public void attemptToPushAnalytics(String deviceId) {
    if ( analytics_output == null ) {
      init();
    }
    if ( analytics_output != null && !is_logging_paused.get() ) {
      try {
        File log_file = new File(directory_analytics, log_file_name);
        long len = log_file.length();
        // If over 50kb, push log file
        if (len > log_file_push_limit) {
          writeToAnalyticsServer(deviceId);
        }
      } catch (Exception e) {
        LOG.debug("Ignoring exception: attemptToPushAnalytics raised: ", e);
      }
    }
  }

  public void logToAnalytics(String message) {
    if ( analytics_output == null ) {
      init();
    }
    if ( analytics_output != null && !is_logging_paused.get() ) {
      try {
        String date_str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,").format(new Date());
        analytics_output.write(date_str + message + "\n");
        analytics_output.flush();  // Make small synchronous additions for now
      } catch (Exception e) {
        LOG.debug("Ignoring exception: logToAnalytics raised: ", e);
      }
    }
  }
}
