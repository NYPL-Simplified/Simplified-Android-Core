package org.nypl.simplified.tests.books.idle_timer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType;
import org.nypl.simplified.tests.EventAssertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.subjects.PublishSubject;

public abstract class ProfileIdleTimerContract {

  private ExecutorService exec_single;
  private ExecutorService exec_multi;
  private PublishSubject<ProfileEvent> events;
  private List<ProfileEvent> event_log;

  @Before
  public final void setUp()
  {
    this.exec_single = Executors.newFixedThreadPool(1);
    this.exec_multi = Executors.newCachedThreadPool();
    this.events = PublishSubject.create();
    this.event_log = Collections.synchronizedList(new ArrayList<>());
  }

  @After
  public final void tearDown()
  {
    this.exec_single.shutdown();
    this.exec_multi.shutdown();
  }

  @Test
  public void testIdleTimerEventSingle() throws Exception {

    this.events.subscribe(this.event_log::add);

    final ProfileIdleTimerType timer =
        ProfileIdleTimer.create(this.exec_single, this.events);

    timer.setMaximumIdleSeconds(1);
    timer.start();

    Thread.sleep(2L * 1000L);

    EventAssertions.isType(ProfileIdleTimeOutSoon.class, this.event_log, 0);
    EventAssertions.isType(ProfileIdleTimedOut.class, this.event_log, 1);
    Assert.assertEquals(1, timer.currentIdleSeconds());
    Assert.assertEquals(1, timer.maximumIdleSeconds());
  }

  @Test
  public void testIdleTimerReset() throws Exception {

    this.events.subscribe(this.event_log::add);

    final ProfileIdleTimerType timer =
        ProfileIdleTimer.create(this.exec_single, this.events);

    timer.setMaximumIdleSeconds(2);
    timer.start();

    for (int index = 0; index < 5; ++index) {
      timer.reset();
      Assert.assertEquals(0, timer.currentIdleSeconds());
      Assert.assertEquals(2, timer.maximumIdleSeconds());
      Thread.sleep(800L);
    }

    EventAssertions.isType(ProfileIdleTimeOutSoon.class, this.event_log, 0);
    EventAssertions.isType(ProfileIdleTimeOutSoon.class, this.event_log, 1);
    EventAssertions.isType(ProfileIdleTimeOutSoon.class, this.event_log, 2);
    EventAssertions.isType(ProfileIdleTimeOutSoon.class, this.event_log, 3);
  }

  @Test
  public void testIdleTimerStop() throws Exception {

    this.events.subscribe(this.event_log::add);

    final ProfileIdleTimerType timer =
        ProfileIdleTimer.create(this.exec_single, this.events);

    timer.setMaximumIdleSeconds(2);
    timer.start();
    timer.stop();

    Thread.sleep(4L * 1000L);

    Assert.assertEquals(0, this.event_log.size());
  }

  @Test
  public void testIdleTimerSetMinutes() throws Exception {

    this.events.subscribe(this.event_log::add);

    final ProfileIdleTimerType timer =
        ProfileIdleTimer.create(this.exec_single, this.events);

    timer.setMaximumIdleMinutes(60);

    Assert.assertEquals(0, this.event_log.size());
    Assert.assertEquals(0, timer.currentIdleSeconds());
    Assert.assertEquals(60 * 60, timer.maximumIdleSeconds());
  }
}
