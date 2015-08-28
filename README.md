Simplified
==========

© NYPL Labs 2015

## Building

For some reason, Google have decided against continuing to deploy
Android artifacts to Maven Central, so it's necessary to deploy
them manually into a local repository so that the dependencies
specified in the simplified-app project can be resolved. Doing this
is out of the scope of this documentation, but is achieved with
the following tool from Simpligility:

https://github.com/simpligility/maven-android-sdk-deployer

Once the Android artifacts are deployed, and your copy of Maven
knows how to find the repository in which they were deployed, it's
necessary to set `$ANDROID_HOME` to the location of the Android SDK
tools. For example, if the SDK is at `${HOME}/local/android-sdk-linux`,
then:

```
$ export ANDROID_HOME=${HOME}/local/android-sdk-linux
```

If the package should be built without support for Adobe DRM,
simply run:

```
$ mvn clean package
```

If the application is to be built with support for Adobe DRM, the
the Adobe-provided `ReaderClientCert.sig` file must be placed in
`simplified-app/src/main/assets`. The build will check for the
existence of this file and fail if it does not exist. Additionally,
the NYPL Adobe DRM package(s) must be deployed to a local repository.

Once this is done, building the package with Adobe DRM support
enabled is achieved by:

```
$ mvn -P nypl-drm-adobe clean package
```

## Branding

To produce NYPL-branded packages:

* Edit `simplified-app/src/main/AndroidManifest.xml.`

Change:

```
<application
  android:name="org.nypl.simplified.app.Simplified"
  android:allowBackup="true"
  android:icon="@drawable/simplified"
  android:label="@string/app_name"
  android:largeHeap="true"
  android:theme="@style/SimplifiedTheme">
```
To:

```
<application
  android:name="org.nypl.simplified.app.Simplified"
  android:allowBackup="true"
  android:icon="@drawable/nypl"
  android:label="@string/app_name"
  android:largeHeap="true"
  android:theme="@style/SimplifiedTheme">
```

* Edit `simplified-app/src/main/res/values/colors.xml`

Change:
```
<color name="main_color">#f0731f</color>
```
To:
```
<color name="main_color">@color/nypl_brand_color</color>
```

# Releases

Release procedure:

* Disable debugging options in `simplified-app/src/main/res/values/debug.xml.`

* Set log level to `INFO` in `simplified-app/src/main/assets/logback.xml.`

  Note that the application logs *a lot*, and therefore debug
  logging has a noticeable effect on UI performance. With the log
  level set to `INFO` or above, logging is completely disabled
  and does not incur a measurable cost at run-time (log statements
  are reduced to the equivalent of a single conditional branch and
  empty method call; no I/O or log message formatting occurs).

* Update the version strings in all of the Android manifests:

```
$ vim `find . -name AndroidManifest.xml -type f | grep src`
```

* Update the version strings in the Maven POMs (where
   `$NEW_VERSION_NUMBER` is the new version).

```
$ mvn versions:set -DnewVersion=$NEW_VERSION_NUMBER
```

* Run a build, ensure all tests pass.

* Tag in git and pushed signed archives and APK files to wherever they
   need to go.

# Development

This project is developed using the
[git-flow](http://nvie.com/posts/a-successful-git-branching-model/)
branching model.

The [gitflow AVH edition](https://github.com/petervanderdoes/gitflow/)
tool is used to facilitate this. The `master` branch represents the
current production-ready code, and development occurs on the `develop`
branch. All commits and tags are PGP-signed for stronger assurances
of authenticity.

