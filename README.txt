Simplified

© NYPL Labs 2015
------------------------------------------------------------------------

Current build instructions:

  For some reason, Google have decided against continuing to deploy
  Android artifacts to Maven Central, so it's necessary to deploy
  them manually into a local repository so that the dependencies
  specified in the simplified-app project can be resolved. Doing this
  is out of the scope of this documentation, but is achieved with
  the following tool from Simpligility:

    https://github.com/simpligility/maven-android-sdk-deployer

  Once the Android artifacts are deployed, and your copy of Maven
  knows how to find the repository in which they were deployed, it's
  necessary to set $ANDROID_HOME to the location of the Android SDK
  tools. For example, if the SDK is at ${HOME}/local/android-sdk-linux,
  then:

    $ export ANDROID_HOME=${HOME}/local/android-sdk-linux

  Then, simply:

    $ mvn -C clean package

