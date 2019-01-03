package org.nypl.simplified.books.idle_timer;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default implementation of the {@link ProfileIdleTimerType} interface.
 */

public final class ProfileIdleTimer implements ProfileIdleTimerType {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileIdleTimer.class);

  private final ExecutorService exec;
  private final ObservableType<ProfileEvent> events;
  private final AtomicInteger seconds_maximum;
  private final AtomicInteger seconds_warning;
  private final AtomicReference<TimerTask> running;

  private ProfileIdleTimer(
      final ExecutorService exec,
      final ObservableType<ProfileEvent> events) {

    this.exec = NullCheck.notNull(exec, "Exec");
    this.events = NullCheck.notNull(events, "Events");
    this.seconds_maximum = new AtomicInteger(60 * 10);
    this.seconds_warning = new AtomicInteger(60);
    this.running = new AtomicReference<>();
  }

  /**
   * Create a new idle timer.
   *
   * @param exec   The executor that will be used for the timer thread
   * @param events An observable that will receive timeout events
   * @return A new timer
   */

  public static ProfileIdleTimerType create(
      final ExecutorService exec,
      final ObservableType<ProfileEvent> events) {
    return new ProfileIdleTimer(exec, events);
  }

  @Override
  public void start() {
    this.stop();

    final TimerTask task = new TimerTask(this.seconds_warning, this.seconds_maximum, this.events);
    this.running.set(task);
    this.exec.submit(task);
  }

  @Override
  public void stop() {
    final TimerTask task = this.running.get();
    if (task != null) {
      task.cancelled.set(true);
    }
    this.running.set(null);
  }

  @Override
  public void reset() {
    final TimerTask task = this.running.get();
    if (task != null) {
      task.seconds_elapsed.set(0);
      task.warned.set(false);
    }
  }

  @Override
  public void setWarningIdleSecondsRemaining(final int time) {
    this.seconds_warning.set(Math.max(1, time));
  }

  @Override
  public void setMaximumIdleSeconds(final int time) {
    this.seconds_maximum.set(Math.max(1, time));
  }

  @Override
  public int maximumIdleSeconds() {
    return this.seconds_maximum.get();
  }

  @Override
  public int currentIdleSeconds() {
    final TimerTask task = this.running.get();
    if (task != null) {
      return task.seconds_elapsed.get();
    }
    return 0;
  }

  private static final class TimerTask implements Runnable {

    private final AtomicBoolean cancelled;
    private final AtomicInteger seconds_elapsed;
    private final AtomicInteger seconds_warning;
    private final AtomicInteger seconds_maximum;
    private final ObservableType<ProfileEvent> events;
    private final AtomicBoolean warned;

    TimerTask(
        final AtomicInteger seconds_warning,
        final AtomicInteger seconds_maximum,
        final ObservableType<ProfileEvent> events)
    {
      this.seconds_warning = seconds_warning;
      this.seconds_maximum = seconds_maximum;
      this.seconds_elapsed = new AtomicInteger(0);
      this.events = events;
      this.cancelled = new AtomicBoolean(false);
      this.warned = new AtomicBoolean(false);
    }

    @Override
    public void run() {
      LOG.debug("start");

      try {
        while (true) {
          if (this.cancelled.get()) {
            LOG.debug("cancelled");
            break;
          }

          final int elapsed = this.seconds_elapsed.get();
          final int maximum = this.seconds_maximum.get();
          if (elapsed >= maximum) {
            this.events.send(ProfileIdleTimedOut.get());
            LOG.debug("timed out");
            break;
          }

          if (maximum - elapsed <= this.seconds_warning.get() && !this.warned.get()) {
            this.warned.set(true);
            this.events.send(ProfileIdleTimeOutSoon.get());
            LOG.debug("time out warning published");
          }

          this.seconds_elapsed.incrementAndGet();

          try {
            Thread.sleep(1000L);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      } finally {
        LOG.debug("finished");
      }
    }
  }
}
