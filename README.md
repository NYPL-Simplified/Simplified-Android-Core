Library Simplified
===

[![Build Status](https://img.shields.io/travis/com/NYPL-Simplified/android?style=flat-square)](https://www.travis-ci.com/NYPL-Simplified/android)

The NYPL's Library Simplified Android client.

![simplified](./src/site/resources/simplified.jpg?raw=true)

_Image by [Predrag Kezic](https://pixabay.com/users/PredragKezic-582203/?utm_source=link-attribution&utm_medium=referral&utm_campaign=image&utm_content=581229) from [Pixabay](https://pixabay.com/?utm_source=link-attribution&utm_medium=referral&utm_campaign=image&utm_content=581229)_

### Building

#### Build!

The short version: Install an [Android SDK](#android-sdk), configure [credentials for Nexus](#credentials),
and run:

~~~
$ ./gradlew clean assemble test
~~~

Please read the list of instructions below for specific details on configurations.

#### Android SDK

Install the [Android SDK and Android Studio](https://developer.android.com/studio/). We don't
support the use of any other IDE at the moment.

#### JDK

Install a reasonably modern JDK: Java 8 is the current recommendation for Android Studio.
You must have Java SE 8u101 or [a newer version of Java SE 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
installed. This is because older versions of Java do not trust
[Let's Encrypt](https://letsencrypt.org/) which provides the SSL certificate on our Nexus
repository instance.

The `JAVA_HOME` environment variable must be set correctly. You can check what it is set to in
most shells with `echo $JAVA_HOME`. If that command does not show anything, adding the following
line to `$HOME/.profile` and then executing `source $HOME/.profile` or opening a new shell
should suffice:

~~~w
# Replace NNN with your particular version of 1.8.0.
export JAVA_HOME=/path/to/jdk1.8.0_NNN
~~~

You can verify that everything is set up correctly by inspecting the results of both
`java -version` and `javac -version`:

~~~
$ java -version
openjdk version "1.8.0_222"
OpenJDK Runtime Environment (build 1.8.0_222-b05)
OpenJDK 64-Bit Server VM (build 25.222-b05, mixed mode)
~~~

#### Credentials

Our application currently needs packages that are only available from our Nexus server in order
to build correctly. (This will be changed in the future when non-DRM-enabled variants of the app
are officially supported.) Nexus credentials can be obtained by emailing `malcolmwoods@nypl.org`
or by asking in the `#mobile-development` channel of
[librarysimplified.slack.com](https://librarysimplified.slack.com).

Once you have your credentials, the following lines must be added to `$HOME/.gradle/gradle.properties`:

~~~
# Replace USERNAME and PASSWORD appropriately.
# Do NOT use quotes around either value.
org.librarysimplified.nexus.username=USERNAME
org.librarysimplified.nexus.password=PASSWORD
~~~

#### Adobe Certificate

The project currently builds, amongst other things, a version of the SimplyE application that
supports Adobe DRM. In order to use the DRM,  the correct certificate file must be placed at
`simplified-app-simplye/src/main/assets/ReaderClientCert.sig` in order for Adobe DRM to work.
If you fail to place the certificate at this location, the application will refuse to build
with the following error:

~~~
You are attempting to build with Adobe DRM but have not added the required
certificate file at ./simplified-app-simplye/src/main/assets/ReaderClientCert.sig

If you do not want to use Adobe DRM, define the following in ./gradle.properties:

org.librarysimplified.with_drm_adobe = false
~~~

As explained by the error message, if you want to build the applications _without_ Adobe DRM
support, it is necessary to set the `org.librarysimplified.with_drm_adobe` property to `false`
in the `gradle.properties` file at the root of the project.

#### Findaway Support

The project currently builds, amongst other things, a version of the SimplyE application that
supports Findaway audio books. This requires a Findaway license.

If you want to build the applications _without_ support for Findaway audio book support, it is
necessary to set the `org.librarysimplified.with_findaway` property to `false` in the
`gradle.properties` file at the root of the project.

#### Bugsnag Support

The project currently builds, amongst other things, a version of the SimplyE application that
supports error reporting via [Bugsnag](https://www.bugsnag.com/). This requires that a configuration
file be placed at `simplified-app-simplye/src/main/assets/bugsnag.conf` containing your Bugsnag
API token:

~~~
bugsnag.api_token = 1234123412341234
~~~

If you want to build the applications _without_ support for Bugsnag, it is
necessary to set the `org.librarysimplified.with_bugsnag` property to `false` in the
`gradle.properties` file at the root of the project.

#### APK signing

If you wish to generate a signed APK for publishing the application, you will need to copy
a keystore to `simplified-app-simplye/keystore.jks` and set the following values correctly in
`$HOME/.gradle/gradle.properties`:

~~~
org.librarysimplified.simplye.keyAlias=
org.librarysimplified.simplye.keyPassword=
org.librarysimplified.simplye.storePassword=
~~~

### Project Structure

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

Most of the modularity concepts described here were pioneered by the [OSGi module system](https://www.osgi.org/developer/modularity/)
and so, although the Library Simplified application is not an OSGi application, much of the
design and architecture conforms to conventions followed by OSGi applications. Further reading
can be found on the OSGi web site.

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
|[org.librarysimplified.accounts.source.resolution](simplified-accounts-source-resolution)|Standard account provider resolution logic|
|[org.librarysimplified.accounts.source.spi](simplified-accounts-source-spi)|Account provider source SPI|
|[org.librarysimplified.analytics.api](simplified-analytics-api)|Analytics API|
|[org.librarysimplified.analytics.circulation](simplified-analytics-circulation)|Circulation manager analytics implementation|
|[org.librarysimplified.analytics.lfa](simplified-analytics-lfa)|LibraryForAll analytics implementation|
|[org.librarysimplified.app.shared](simplified-app-shared)|Shared application module|
|[org.librarysimplified.app.simplye](simplified-app-simplye)|SimplyE Application|
|[org.librarysimplified.app.vanilla](simplified-app-vanilla)|Vanilla application|
|[org.librarysimplified.app.vanilla.with_profiles](simplified-app-vanilla-with-profiles)|Vanilla application [with profiles enabled]|
|[org.librarysimplified.books.api](simplified-books-api)|Book types|
|[org.librarysimplified.books.bundled.api](simplified-books-bundled-api)|Bundled books API|
|[org.librarysimplified.books.controller](simplified-books-controller)|Books/Profiles controller implementation|
|[org.librarysimplified.books.controller.api](simplified-books-controller-api)|Books controller API|
|[org.librarysimplified.books.covers](simplified-books-covers)|Book cover loading and generation|
|[org.librarysimplified.books.database](simplified-books-database)|Book database implementation|
|[org.librarysimplified.books.database.api](simplified-books-database-api)|Book database API|
|[org.librarysimplified.books.registry.api](simplified-books-registry-api)|Book registry API|
|[org.librarysimplified.boot.api](simplified-boot-api)|Application boot API|
|[org.librarysimplified.branding](simplified-branding)|Branding functionality|
|[org.librarysimplified.bugsnag](simplified-bugsnag)|Bugsnag functionality|
|[org.librarysimplified.cardcreator](simplified-cardcreator)|NYPL card creator|
|[org.librarysimplified.datepicker](simplified-datepicker)|Date picker UI component|
|[org.librarysimplified.datepicker.demo](simplified-datepicker-demo)|Date picker UI component demo|
|[org.librarysimplified.documents](simplified-documents)|Documents API|
|[org.librarysimplified.downloader.core](simplified-downloader-core)|Downloader|
|[org.librarysimplified.feeds.api](simplified-feeds-api)|Feed API|
|[org.librarysimplified.files](simplified-files)|File utilities|
|[org.librarysimplified.futures](simplified-futures)|Guava Future extensions|
|[org.librarysimplified.http.core](simplified-http-core)|HTTP API|
|[org.librarysimplified.json.core](simplified-json-core)|JSON utilities|
|[org.librarysimplified.migration.api](simplified-migration-api)|Data migration API|
|[org.librarysimplified.migration.fake](simplified-migration-fake)|Fake data migration for testing purposes|
|[org.librarysimplified.migration.from3master](simplified-migration-from3master)|Data migration from 3.0.0 master branch data|
|[org.librarysimplified.migration.spi](simplified-migration-spi)|Data migration SPI|
|[org.librarysimplified.mime](simplified-mime)|MIME type handling|
|[org.librarysimplified.notifications](simplified-notifications)|Notification service|
|[org.librarysimplified.observable](simplified-observable)|Lightweight, type-safe Observable implementation|
|[org.librarysimplified.opds.auth_document](simplified-opds-auth-document)|OPDS authentication document parser implementation|
|[org.librarysimplified.opds.auth_document.api](simplified-opds-auth-document-api)|OPDS authentication document parser API|
|[org.librarysimplified.opds.core](simplified-opds-core)|OPDS feed parser|
|[org.librarysimplified.parser.api](simplified-parser-api)|Parser API|
|[org.librarysimplified.patron](simplified-patron)|Patron user profile parser implementation|
|[org.librarysimplified.patron.api](simplified-patron-api)|Patron user profile parser API|
|[org.librarysimplified.prefs](simplified-prefs)|Legacy preferences handler|
|[org.librarysimplified.presentableerror.api](simplified-presentableerror-api)|Presentable error API|
|[org.librarysimplified.profiles](simplified-profiles)|Profile database implementation|
|[org.librarysimplified.profiles.api](simplified-profiles-api)|Profile database API|
|[org.librarysimplified.profiles.controller.api](simplified-profiles-controller-api)|Profile controller API|
|[org.librarysimplified.reader.api](simplified-reader-api)|Reader API types|
|[org.librarysimplified.reader.bookmarks](simplified-reader-bookmarks)|Reader bookmark service implementation|
|[org.librarysimplified.reader.bookmarks.api](simplified-reader-bookmarks-api)|Reader bookmark service API|
|[org.librarysimplified.reports](simplified-reports)|Error reporting|
|[org.librarysimplified.splash](simplified-splash)|Splash screen|
|[org.librarysimplified.stack](simplified-stack)|Immutable stack data structure|
|[org.librarysimplified.taskrecorder.api](simplified-taskrecorder-api)|Task recorder API|
|[org.librarysimplified.tenprint](simplified-tenprint)|10PRINT implementation|
|[org.librarysimplified.tests](simplified-tests)|Test suite|
|[org.librarysimplified.tests.android](simplified-tests-android)|On-device test suite|
|[org.librarysimplified.tests.sandbox](simplified-tests-sandbox)|Sandbox for informal testing|
|[org.librarysimplified.tests.strings](simplified-tests-strings)|Mock strings for test suites|
|[org.librarysimplified.theme](simplified-theme)|Application theme functionality|
|[org.librarysimplified.threads](simplified-threads)|Thread utilities|

### License

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
