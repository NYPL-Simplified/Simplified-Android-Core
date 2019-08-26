### Project Structure

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

## Java Prerequisites

1. You must have Java SE 8u101 or [a newer version of Java SE 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed. This is because older versions of Java do not trust [Let's Encrypt](https://letsencrypt.org/) which provides our SSL certificate.

2. The `JAVA_HOME` environment variable must be set correctly. You can check what it is set to in most shells with `echo $JAVA_HOME`. If that command does not show anything, adding the following line to `~/.profile` (assuming you are on macOS) and then executing `source ~/.profile` or opening a new shell should suffice:

~~~w
# Replace NNN with your particular version of 1.8.0.
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_NNN.jdk/Contents/Home
~~~

3. You can verify that everything is set up correctly by inspecting the results of both `java -version` and `javac -version`.

## Android Studio Prerequisites

The latest version of Android Studio is strongly recommended. Versions older than 2.3.3 have not been tested.

## Nexus Setup

Our application currently needs packages that are only available from our Nexus server in order to build correctly. (This will be changed in the future when non-DRM-enabled variants of the app are officially supported.) Nexus credentials can be obtained by emailing `nypl@winniequinn.com` or by asking in the `#simplified-android` channel of [librarysimplified.slack.com](https://librarysimplified.slack.com).

Once you have your credentials, the following lines must be added to `~/.gradle/gradle.properties`:

~~~
# Replace USERNAME and PASSWORD appropriately.
# Do NOT use quotes around either value.
org.librarysimplified.nexus.username=USERNAME
org.librarysimplified.nexus.password=PASSWORD
~~~

## Adobe Certificate Setup

The correct certificate file must be placed at `simplified-app-simplye/src/main/assets/ReaderClientCert.sig` in order for Adobe DRM to work. The app will function correctly without this file so long as only non-DRM-protected books are opened.

For Zendesk, you should use the following configuration:

For Salesforce Desk, use the following instead:

## Generating Signed APKs

If you wish to generate a signed APK for publishing the application, you will need to set the following values correctly in `~/.gradle/gradle.properties`:

~~~
org.librarysimplified.simplye.keyAlias=
org.librarysimplified.simplye.keyPassword=
org.librarysimplified.simplye.storePassword=
~~~

In addition, you will need to obtain the correct Java keystore and either place it in the project at `simplified-app-simplye/keystore.jks` or create a symbolic link at the same location appropriately. All files matching `*.jks` are set to be ignored by Git, but care should always be taken to ensure keystores and other secrets are never committed regardless.

Once the above has been completed, executing `./gradlew assembleRelease` will generate the signed APK and place it at `./simplified-app-simplye/build/outputs/apk/simplified-app-simplye-release.apk`.

## Branding And Configurable Features

See [simplified-app-shared/README-Branding.md](simplified-app-shared/README-Branding.md)
for documentation on how to customize branding of the application.

# Building

**NOTE:** Due to an unknown issue, you must execute `./gradlew assembleDebug` one time before opening the project in Android Studio. This will pull in all dependencies that, for whatever reason, are not fetched if Gradle is executed via Android Studio.

After setup is complete, the project can be opened in Android Studio and built as normal.
