Open eBooks
===

The NYPL's [Open eBooks](https://play.google.com/store/apps/details?id=org.nypl.labs.OpenEbooks.app) Android application.

### Building

#### Build!

The short version: Install an [Android SDK](#android-sdk), configure
[credentials for Nexus](#credentials), add [the required configuration files](#configuration-files), add a
[keystore](#apk-signing) and run:

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

#### Configuration Files

The application requires a range of configuration files for the various
services. All of these are available in the private NYPL Certificates
repository.

|Source|Copy To|Description|
|------|-------|-----------|
|[google-services.json](https://github.com/NYPL-Simplified/Certificates/blob/master/SimplyE/Android/google-services.json)|`app/google-services.json`|Crashlytics service configuration|
|[keystore.jks](https://github.com/NYPL-Simplified/Certificates/blob/master/APK%20Signing/nypl-keystore.jks)|`app/keystore.jks`|NYPL APK signing keystore|
|[ReaderClientCert.sig](https://github.com/NYPL-Simplified/Certificates/tree/master/OpenEbooks/Android/ReaderClientCert.sig)|`app/src/main/assets/ReaderClientCert.sig`|Adobe DRM certificate|

#### Credentials

Our application currently needs packages that are only available from
our Nexus server in order to build correctly. This will be changed
in the future when the application dependencies are pushed to [Maven
Central](https://search.maven.org/).

Nexus credentials can be obtained by emailing `malcolmwoods@nypl.org`
or by asking in the `#mobile-development` channel of
[librarysimplified.slack.com](https://librarysimplified.slack.com).

Once you have your credentials, the following lines must be added to `$HOME/.gradle/gradle.properties`:

~~~
# Replace USERNAME and PASSWORD appropriately.
# Do NOT use quotes around either value.
org.librarysimplified.nexus.username=USERNAME
org.librarysimplified.nexus.password=PASSWORD
~~~

#### APK signing

For signing releases, a keystore must be copied to
`app/keystore.jks` and the following values set correctly in
`$HOME/.gradle/gradle.properties`:

~~~
# Replace KEYALIAS, KEYPASSWORD, and STOREPASSWORD appropriately.
# Do NOT use quotes around values.
org.librarysimplified.simplye.keyAlias=KEYALIAS
org.librarysimplified.simplye.keyPassword=KEYPASSWORD
org.librarysimplified.simplye.storePassword=STOREPASSWORD
~~~

See the [nypl-keystore.properties](https://github.com/NYPL-Simplified/Certificates/blob/master/APK%20Signing/nypl-keystore.properties) file
for the correct values.

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
