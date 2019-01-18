package org.nypl.simplified.splash

sealed class SplashEvent {

  sealed class SplashImageEvent : SplashEvent() {
    data class SplashImageTimedOut(
      val unused: Int)
      : SplashImageEvent()
  }

  sealed class SplashEULAEvent : SplashEvent() {
    data class SplashEULAAgreed(
      val unused: Int)
      : SplashEULAEvent()

    data class SplashEULADisagreed(
      val unused: Int)
      : SplashEULAEvent()
  }

}
