Library Simplified
===

[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-Android-Core/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.librarysimplified/org.librarysimplified.main?style=flat-square)](https://repo1.maven.org/maven2/org/librarysimplified/)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.librarysimplified/org.librarysimplified.main.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/org.librarysimplified/)

The NYPL's [Library Simplified](http://www.librarysimplified.org/) Android client.

![simplified](./src/site/resources/simplified.jpg?raw=true)

_Image by [Predrag Kezic](https://pixabay.com/users/PredragKezic-582203/?utm_source=link-attribution&utm_medium=referral&utm_campaign=image&utm_content=581229) from [Pixabay](https://pixabay.com/?utm_source=link-attribution&utm_medium=referral&utm_campaign=image&utm_content=581229)_

|Build|Status|
|-----|------|
|[Nightly, DRM, JDK 11](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-Android-Core/Android%20CI%20(Daily%20Authenticated,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+Authenticated%2C+JDK+11%29%22)|
|[Nightly, DRM-Free, JDK 11](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+11%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-Android-Core/Android%20CI%20(Daily%20DRM-Free,%20JDK%2011)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+11%29%22)|
|[Nightly, DRM-Free, JDK 15](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+15%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-Android-Core/Android%20CI%20(Daily%20DRM-Free,%20JDK%2015)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Daily+DRM-Free%2C+JDK+15%29%22)|
|[Last Commit](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/Simplified-Android-Core/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/Simplified-Android-Core/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)|

### What Is This?

The contents of this repository provide the framework of an application used to build,
amongst others, the NYPL's official [SimplyE](https://www.nypl.org/books-music-movies/ebookcentral/simplye)
application. The framework provides a base application with numerous configuration
switches, and configurable _branding_. The expectation is that third parties will
produce final builds of applications by defining _application frontends_ that specify
dependencies on the framework, custom color schemes, and logos. 

The repository contains a number of applications that are all built from the
same core:

|Application|Module|Description|
|-----------|------|-----------|
|Vanilla|[simplified-app-vanilla](simplified-app-vanilla)|DRM-free generic reading application|
|SimplyE|[simplified-app-simplye](simplified-app-simplye)|The NYPL's official [SimplyE](https://www.nypl.org/books-music-movies/ebookcentral/simplye) application|
|Open eBooks|[simplified-app-openebooks](simplified-app-openebooks)|The [Open eBooks](https://openebooks.net/) application|

## Contents

* [Building](#building-the-code)
  * [The Short Version](#the-short-version)
  * [The Longer Version](#the-longer-version)
    * [Android SDK](#android-sdk)
    * [JDK](#jdk)
    * [Nexus Credentials](#nexus-credentials)
    * [APK Signing](#apk-signing)
    * [Enabling DRM](#enabling-drm)
    * [Adobe DRM](#adobe-drm-support)
    * [Findaway DRM](#findaway-audiobook-drm-support)
* [Development](#development)
  * [Branching/Merging](#branchingmerging)
  * [Project Structure](#project-structure--architecture)
    * [MVC](#mvc)
    * [MVVM](#mvvm)
    * [API vs SPI](#api-vs-spi)
    * [Modules](#modules)
  * [Binaries](#binaries)
  * [Ktlint](#ktlint)
* [Release Process](#release-process)
* [License](#license)

## Building The Code

#### The Short Version

Install an [Android SDK](#android-sdk) and a [JDK](#jdk) and run:

~~~
$ ./gradlew clean ktlint assembleDebug test
~~~

This will build all of the code and run the unit tests, but only the
[Vanilla](simplified-app-vanilla) application will be built by default. In
order to build the other applications such as [SimplyE](simplified-app-simplye),
it's necessary to obtain the correct [credentials](#nexus-credentials) from the
NYPL and [enable DRM](#enabling-drm).

#### The Longer Version

#### Android SDK

Install the [Android SDK and Android Studio](https://developer.android.com/studio/). We don't
support the use of any other IDE at the moment.

#### JDK

Install a reasonably modern JDK: Java 11 is the current long-term support (LTS) release of Java. We
perform nightly builds using the current LTS Java release, and the current bleeding-edge Java
release in order to try to detect any upcoming compatibility issues, but we don't recommend building
on anything other than the current LTS JDK for everyday usage.

Any of the following JDKs should work:

  * [OpenJDK](https://jdk.java.net/java-se-ri/11)
  * [Adoptium](https://adoptopenjdk.net/)
  * [Amazon Coretto](https://aws.amazon.com/corretto/)
  * [Zulu](https://www.azul.com/downloads/zulu-community/?package=jdk)

The `JAVA_HOME` environment variable must be set correctly. You can check what it is set to in
most shells with `echo $JAVA_HOME`. If that command does not show anything, adding the following
line to `$HOME/.profile` and then executing `source $HOME/.profile` or opening a new shell
should suffice:

~~~w
# Replace NNN with your particular version of 11.
export JAVA_HOME=/path/to/jdk-11+NNN
~~~

You can verify that everything is set up correctly by inspecting the results of both
`java -version` and `javac -version`:

~~~
$ java -version
openjdk version "11.0.8" 2020-07-14
OpenJDK Runtime Environment (build 11.0.8+10)
OpenJDK 64-Bit Server VM (build 11.0.8+10, mixed mode)
~~~

#### Nexus Credentials

Our application can use packages that are only available from our
Nexus server. If you wish to use these packages, you need to obtain
Nexus credentials and then tell Gradle to use them.

Nexus credentials can be obtained by emailing `malcolmwoods@nypl.org`
or by asking in the `#mobile-development` channel of
[librarysimplified.slack.com](https://librarysimplified.slack.com).

Once you have your credentials, the following lines must be added to `$HOME/.gradle/gradle.properties`:

~~~
# Replace USERNAME and PASSWORD appropriately.
# Do NOT use quotes around either value.
org.librarysimplified.nexus.username=USERNAME
org.librarysimplified.nexus.password=PASSWORD
org.librarysimplified.nexus.depend=true
~~~

#### APK signing

If you wish to generate a signed APK for publishing the Vanilla application, you will need to copy
a keystore to `release.jks` and set the following values correctly in
`$HOME/.gradle/gradle.properties`:

~~~
# Replace KEYALIAS, KEYPASSWORD, and STOREPASSWORD appropriately.
# Do NOT use quotes around values.
org.librarysimplified.keyAlias=KEYALIAS
org.librarysimplified.keyPassword=KEYPASSWORD
org.librarysimplified.storePassword=STOREPASSWORD
~~~

Note that APK files are only signed if the code is built in _release_ mode. In other words, you
need to use either of these commands to produce signed APK files:

~~~
$ ./gradlew clean assembleRelease test
$ ./gradlew clean assemble test
~~~

#### Enabling DRM

The application contains optional support for various DRM systems, and these
must be enabled explicitly in order to build [SimplyE](simplified-app-simplye).

Firstly, make sure you have your [Nexus](#nexus-credentials) credentials
correctly configured. Then, add the following property to your
`$HOME/.gradle/gradle.properties` file:

```
org.librarysimplified.drm.enabled=true
```

This will instruct the build system that you want to build with DRM enabled.
If you were to attempt to build the code right now, you would encounter a
build failure: When DRM is enabled, the build system will check that you have
provided various configuration files containing secrets that the DRM systems
require, and will refuse to build the app if you've failed to do this. The
build system can copy in the correct secrets for you if tell it the location
of directories containing those secrets. For example, assuming that you have
[SimplyE's](simplified-app-simplye) secrets in `/path/to/simplye/secrets` and
[Open eBook's](simplified-app-openebooks) secrets in `/path/to/openebooks/secrets`,
you can add the following properties to your `$HOME/.gradle/gradle.properties` file
and the build system will copy in the required secrets at build time:

```
org.librarysimplified.app.assets.openebooks=/path/to/openebooks/secrets
org.librarysimplified.app.assets.simplye=/path/to/simplye/secrets
```

#### Adobe DRM Support

The project currently makes calls to the NYPL's [Adobe DRM
API](https://github.com/NYPL-Simplified/DRM-Android-Core). The API
is structured in a manner that means that enabling actual support
for Adobe DRM simply entails adding a dependency on the NYPL's Adobe
DRM _implementation_. This implementation is only available to DRM
licensees. Please get in touch with us if you have a DRM license and
want to produce a DRM-enabled build!

#### Findaway Audiobook DRM support

The project currently uses the NYPL's [AudioBook API](https://github.com/NYPL-Simplified/audiobook-android)
to provide support for playing audio books. The API is structured such
that adding support for new types of audiobooks and playback engines
only involves adding those modules to the classpath. By default, the
application framework only specifies a dependency on the NYPL's DRM-free
audiobook player module, but there is also an NYPL-developed Findaway
module for Findaway licensees. Please get in touch with us if you have
a Findaway license and want to produce a Findaway-enabled build.

## Development

### Branching/Merging

We use [git flow](https://nvie.com/posts/a-successful-git-branching-model/) as our
basis for branching and creating releases. We highly recommend installing
[Git Flow AVH Edition](https://github.com/petervanderdoes/gitflow-avh) to
automate some of the work of branching and tagging. Using `gitflow-avh`
is not required, but by automating the underlying repository operations,
it eliminates the possibility of making mistakes, and keeps the various
branches consistent.

### Project Structure / Architecture

#### MVC

The project, as a whole, roughly follows an [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller)
architecture distributed over the application modules. The _controller_ in the application is
task-based and executes all tasks on a background thread to avoid any possibility of blocking
the Android UI thread.

#### MVVM

Newer application modules, roughly follow an [MVVM](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) architecture.
The _View Model_ in the application exposes reactive properties
and executes all tasks on a background thread. The _View_ observes those properties and updates on the Android UI thread.

#### API vs SPI

The project makes various references to _APIs_ and _SPIs_. _API_ stands for _application
programming interface_ and _SPI_ stands for _service provider interface_.

An _API_ module defines a user-visible contract (or _specification_) for a module; it defines the
data types and abstract interfaces via which the user is expected to make calls in order to make use of a
module. An API module is typically paired with an _implementation_ module that provides concrete
implementations of the API interface types. A good example of this is the accounts database: The
[Accounts database API](simplified-accounts-database-api) declares a set of data types and
interfaces that describe how an accounts database should behave. The [Accounts database](simplified-accounts-database)
_implementation_ module provides an implementation of the described API. Keeping the API
_specification_ strictly separated from the _implementation_ in this manner has a number of benefits:

* Substitutability: When an _API_ has a sufficiently detailed specification, it's possible to
  replace an implementation module with a superior implementation without having to modify
  code that makes calls to the API.

* Testability: Keeping API types strictly separated from implementation types tends to lead to
  interfaces that are easy to mock.

* Understandability: Users of modules can go straight to the _API_ specifications to find out
  how to use them. This cuts down on the amount of archaeological work necessary to learn how
  to use the application's internal interfaces.

An _SPI_ module is similar to an API in that it provides a specification, however the defined
interfaces are expected to be _implemented_ by users rather than _called_ by users directly. An
implementor of an SPI is known as a _service provider_.

A good example of an SPI is the [Account provider source SPI](simplified-accounts-source-spi); the SPI
defines an interface that is expected to be implemented by account provider sources. The
[file-based source](simplified-accounts-source-filebased) module is capable of delivering account
provider descriptions from a bundled asset file. The [registry source](simplified-accounts-source-nyplregistry)
implementation is capable of fetching account provider descriptions from the NYPL's registry
server. Neither the _SPI_ or the implementation modules are expected to be used by application
programmers directly: Instead, implementation modules are loaded using [ServiceLoader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html)
by the [Account provider registry](simplified-accounts-registry), and users interact with the
registry via a [published registry API](simplified-accounts-registry-api). This same design
pattern is used by the [NYPL AudioBook API](https://github.com/NYPL-Simplified/audiobook-android)
to provide a common API into which new audio book players and parsers can be introduced _without
needing to modify application code at all_.

Modules should make every attempt not to specify explicit dependencies on _implementation_ modules.
API and implementation modules should typically only depend on other API modules, leaving the choice
of implementation modules to the final application assembly. In other words, a module should say
"I can work with any module that provides this API" rather than "I depend on implementation `M`
of a particular API". Following this convention allows us to replace module implementation without
having to modify lots of different parts of the application; it allows us to avoid
_strong coupling_ between modules.

Most of the modularity concepts described here were pioneered by the [OSGi module system](https://www.osgi.org/developer/modularity/)
and so, although the Library Simplified application is not an OSGi application, much of the
design and architecture conforms to conventions followed by OSGi applications. Further reading
can be found on the OSGi web site.

#### Build System

The build is driven by the [build.gradle](build.gradle) file in the root of the project,
with the `build.gradle` files in each module typically only listing dependencies (the actual
dependency definitions are defined in the root `build.gradle` file to avoid duplicating version
numbers over the whole project). Metadata used to publish builds (such as Maven group IDs, version
numbers, etc) is defined in the `gradle.properties` file in each module. The [gradle.properties](gradle.properties)
file in the root of the project defines default values that are overridden as necessary by each
module.

#### Test suite

We aggregate all unit tests in the [simplified-tests](simplified-tests) module. Tests should
be written using the JUnit 5 library, although at the time of writing we have [one test](simplified-tests/src/test/java/org/nypl/simplified/tests/webview/CookiesContract.kt)
that still requires JUnit 4 due to the use of [Roboelectric](http://robolectric.org/).

#### Modules

The project is heavily modularized in order to keep the separate application components as loosely
coupled as possible. New features should typically be implemented as new modules.

|Module|Description|
|------|-----------|
|[org.librarysimplified.accounts.api](simplified-accounts-api)|Accounts API|
|[org.librarysimplified.accounts.database](simplified-accounts-database)|Accounts database implementation|
|[org.librarysimplified.accounts.database.api](simplified-accounts-database-api)|Accounts database API|
|[org.librarysimplified.accounts.json](simplified-accounts-json)|Shared JSON classes|
|[org.librarysimplified.accounts.registry](simplified-accounts-registry)|Account provider registry implementation|
|[org.librarysimplified.accounts.registry.api](simplified-accounts-registry-api)|Account provider registry API|
|[org.librarysimplified.accounts.source.filebased](simplified-accounts-source-filebased)|File/asset based registry source implementation|
|[org.librarysimplified.accounts.source.nyplregistry](simplified-accounts-source-nyplregistry)|NYPL registry client implementation|
|[org.librarysimplified.accounts.source.spi](simplified-accounts-source-spi)|Account provider source SPI|
|[org.librarysimplified.adobe.extensions](simplified-adobe-extensions)|Adobe DRM convenience functions|
|[org.librarysimplified.analytics.api](simplified-analytics-api)|Analytics API|
|[org.librarysimplified.analytics.circulation](simplified-analytics-circulation)|Circulation manager analytics implementation|
|[org.librarysimplified.android.ktx](simplified-android-ktx)|Kotlin Android Extensions|
|[org.librarysimplified.announcements](simplified-announcements)|Announcements API|
|[org.librarysimplified.app.vanilla](simplified-app-vanilla)|Vanilla application|
|[org.librarysimplified.books.api](simplified-books-api)|Book types|
|[org.librarysimplified.books.audio](simplified-books-audio)|Audio book support code|
|[org.librarysimplified.books.borrowing](simplified-books-borrowing)|Book borrowing|
|[org.librarysimplified.books.bundled.api](simplified-books-bundled-api)|Bundled books API|
|[org.librarysimplified.books.controller](simplified-books-controller)|Books/Profiles controller implementation|
|[org.librarysimplified.books.controller.api](simplified-books-controller-api)|Books controller API|
|[org.librarysimplified.books.covers](simplified-books-covers)|Book cover loading and generation|
|[org.librarysimplified.books.database](simplified-books-database)|Book database implementation|
|[org.librarysimplified.books.database.api](simplified-books-database-api)|Book database API|
|[org.librarysimplified.books.formats](simplified-books-formats)|Book formats implementation|
|[org.librarysimplified.books.formats.api](simplified-books-formats-api)|Book formats API|
|[org.librarysimplified.books.registry.api](simplified-books-registry-api)|Book registry API|
|[org.librarysimplified.boot.api](simplified-boot-api)|Application boot API|
|[org.librarysimplified.buildconfig.api](simplified-buildconfig-api)|Build-time configuration API|
|[org.librarysimplified.cardcreator](simplified-cardcreator)|NYPL card creator|
|[org.librarysimplified.content.api](simplified-content-api)|Content resolver API|
|[org.librarysimplified.crashlytics](simplified-crashlytics)|Crashlytics|
|[org.librarysimplified.crashlytics.api](simplified-crashlytics-api)|Crashlytics functionality|
|[org.librarysimplified.documents](simplified-documents)|Documents|
|[org.librarysimplified.feeds.api](simplified-feeds-api)|Feed API|
|[org.librarysimplified.files](simplified-files)|File utilities|
|[org.librarysimplified.futures](simplified-futures)|Guava Future extensions|
|[org.librarysimplified.json.core](simplified-json-core)|JSON utilities|
|[org.librarysimplified.links](simplified-links)|Link types|
|[org.librarysimplified.links.json](simplified-links-json)|Link JSON parsing|
|[org.librarysimplified.main](simplified-main)|Main application module|
|[org.librarysimplified.migration.api](simplified-migration-api)|Data migration API|
|[org.librarysimplified.migration.fake](simplified-migration-fake)|Fake data migration for testing purposes|
|[org.librarysimplified.migration.from3master](simplified-migration-from3master)|Data migration from 3.0.0 master branch data|
|[org.librarysimplified.migration.spi](simplified-migration-spi)|Data migration SPI|
|[org.librarysimplified.networkconnectivity](simplified-networkconnectivity)|Network connectivity|
|[org.librarysimplified.networkconnectivity.api](simplified-networkconnectivity-api)|Network connectivity API|
|[org.librarysimplified.notifications](simplified-notifications)|Notification service|
|[org.librarysimplified.oauth](simplified-oauth)|OAuth|
|[org.librarysimplified.opds.auth_document](simplified-opds-auth-document)|OPDS authentication document parser implementation|
|[org.librarysimplified.opds.auth_document.api](simplified-opds-auth-document-api)|OPDS authentication document parser API|
|[org.librarysimplified.opds.core](simplified-opds-core)|OPDS feed parser|
|[org.librarysimplified.parser.api](simplified-parser-api)|Parser API|
|[org.librarysimplified.patron](simplified-patron)|Patron user profile parser implementation|
|[org.librarysimplified.patron.api](simplified-patron-api)|Patron user profile parser API|
|[org.librarysimplified.presentableerror.api](simplified-presentableerror-api)|Presentable error API|
|[org.librarysimplified.profiles](simplified-profiles)|Profile database implementation|
|[org.librarysimplified.profiles.api](simplified-profiles-api)|Profile database API|
|[org.librarysimplified.profiles.controller.api](simplified-profiles-controller-api)|Profile controller API|
|[org.librarysimplified.reader.api](simplified-reader-api)|Reader API types|
|[org.librarysimplified.reader.bookmarks](simplified-reader-bookmarks)|Reader bookmark service implementation|
|[org.librarysimplified.reader.bookmarks.api](simplified-reader-bookmarks-api)|Reader bookmark service API|
|[org.librarysimplified.reports](simplified-reports)|Error reporting|
|[org.librarysimplified.services.api](simplified-services-api)|Application services API|
|[org.librarysimplified.taskrecorder.api](simplified-taskrecorder-api)|Task recorder API|
|[org.librarysimplified.tenprint](simplified-tenprint)|10PRINT implementation|
|[org.librarysimplified.tests](simplified-tests)|Test suite|
|[org.librarysimplified.tests.sandbox](simplified-tests-sandbox)|Sandbox for informal testing|
|[org.librarysimplified.threads](simplified-threads)|Thread utilities|
|[org.librarysimplified.ui.accounts](simplified-ui-accounts)|Accounts UI components|
|[org.librarysimplified.ui.announcements](simplified-ui-announcements)|Announcements UI components|
|[org.librarysimplified.ui.branding](simplified-ui-branding)|Branding functionality|
|[org.librarysimplified.ui.catalog](simplified-ui-catalog)|Catalog components|
|[org.librarysimplified.ui.errorpage](simplified-ui-errorpage)|Error details screen|
|[org.librarysimplified.ui.images](simplified-ui-images)|Image loader API for general image resources|
|[org.librarysimplified.ui.navigation.api](simplified-ui-navigation-api)|Navigation API|
|[org.librarysimplified.ui.onboarding](simplified-ui-onboarding)|Onboarding|
|[org.librarysimplified.ui.tabs](simplified-ui-navigation-tabs)|Tabbed UI|
|[org.librarysimplified.ui.profiles](simplified-ui-profiles)|Profiles UI|
|[org.librarysimplified.ui.screen](simplified-ui-screen)|Screen API|
|[org.librarysimplified.ui.settings](simplified-ui-settings)|Settings screens|
|[org.librarysimplified.ui.splash](simplified-ui-splash)|Splash screen|
|[org.librarysimplified.ui.theme](simplified-ui-theme)|Application theme functionality|
|[org.librarysimplified.ui.thread.api](simplified-ui-thread-api)|UI thread service|
|[org.librarysimplified.viewer.api](simplified-viewer-api)|Viewer API|
|[org.librarysimplified.viewer.audiobook](simplified-viewer-audiobook)|AudioBook viewer|
|[org.librarysimplified.viewer.epub.readium1](simplified-viewer-epub-readium1)|Readium 1 EPUB reader|
|[org.librarysimplified.viewer.epub.readium2](simplified-viewer-epub-readium2)|Readium 2 EPUB reader|
|[org.librarysimplified.viewer.pdf](simplified-viewer-pdf)|PDF reader|
|[org.librarysimplified.viewer.spi](simplified-viewer-spi)|Viewer SPI|

_The above table is generated with [ReadMe.java](src/misc/ReadMe.java)._

### Binaries

Binaries for every commit are built and published in the [binaries](https://github.com/NYPL-Simplified/android-binaries)
repository. Note that these binaries are _not_ to be considered production ready and may have
undergone little or no testing. Use at your own risk!

### Ktlint

The codebase uses [ktlint](https://ktlint.github.io/) to enforce a consistent 
code style. It's possible to ensure that any changes you've made to the code
continue to pass `ktlint` checks by running the `ktlintFormat` task to reformat
source code:

```
$ ./gradlew ktlintFormat
```

## Release Process

Please see [RELEASING.md](RELEASING.md) for documentation on our release
process.

## License

~~~
Copyright 2015 The New York Public Library, Astor, Lenox, and Tilden Foundations

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
~~~
