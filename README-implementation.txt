Implementation Notes
------------------------------------------------------------------------
Test suite

  Due to design flaws in the Android API, all test suites are currently
  divided into three modules.

  The basic problem is that the majority of code to be tested has
  no actual dependencies on the Android API, and therefore it's
  possible (and more pleasant) to run the unit tests on the host
  during development. Obviously, because the code is eventually going
  to be executed on a real Android device, it's also necessary to
  run the tests on a device (or the emulator). Unfortunately, due
  to an amazingly short-sighted design on the part of the Android
  people, any JUnit test case that should be executed on an Android
  device must be defined in a class that extends an Android-specific
  InstrumentationTestCase class!  In other words, just because we
  want to run tests on an Android device, the entire test suite is
  forced to depend upon the Android API.

  Until Android gets a real test runner, the test suites for an
  arbitrary module M are divided into three modules:

    M-tests
    M-tests-junit4
    M-tests-android

  The M-tests module contains all platform-independent tests. The
  M-tests-junit4 module contains a JUnit4 frontend to the tests
  defined in M-tests. The M-tests-android module contains an
  InstrumentationTestCase frontend to the tests defined in M-tests
  (in addition to any Android-specific tests).

  When Google's android-test-kit has matured enough, much of the
  above can be retired.

------------------------------------------------------------------------
