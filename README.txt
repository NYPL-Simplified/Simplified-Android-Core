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

------------------------------------------------------------------------

To produce NYPL-branded packages:

  1. Edit simplified-app/src/main/AndroidManifest.xml.

     Change:

      <application
        android:name="org.nypl.simplified.app.Simplified"
        android:allowBackup="true"
        android:icon="@drawable/simplified"
        android:label="@string/app_name"
        android:largeHeap="true"
        android:theme="@style/SimplifiedTheme">

     To:

      <application
        android:name="org.nypl.simplified.app.Simplified"
        android:allowBackup="true"
        android:icon="@drawable/nypl"
        android:label="@string/app_name"
        android:largeHeap="true"
        android:theme="@style/SimplifiedTheme">

  2. Edit simplified-app/src/main/res/values/colors.xml.

     Change:

       <color name="main_color">#f0731f</color>

     To:

       <color name="main_color">@color/nypl_brand_color</color>

------------------------------------------------------------------------

Release procedure:

  0. Disable debugging options in simplified-app/src/main/res/values/debug.xml.

  1. Set log level to 'INFO' in simplified-app/src/main/assets/logback.xml.

     Note that the application logs *a lot*, and therefore debug
     logging has a noticeable effect on UI performance. With the log
     level set to 'INFO' or above, logging is completely disabled
     and does not incur a measurable cost at run-time (log statements
     are reduced to the equivalent of a single conditional branch and
     empty method call; no I/O or log message formatting occurs).

  2. Update the version strings in all of the Android manifests:

     $ vim `find . -name AndroidManifest.xml -type f | grep src`

  3. Update the version strings in the Maven POMs:

     $ vim pom.xml simplified-*/pom.xml

  4. Run a build, ensure all tests pass.

  5. Tag in git and pushed signed archives and APK files to wherever
     they need to go.
