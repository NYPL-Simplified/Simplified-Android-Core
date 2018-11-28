# Build Setup

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

## HelpStack Setup

**NOTE:** Care should always be taken to ensure HelpStack is functioning correctly after making any configuration changes. Configuration errors or a lack of configuration may result in errors that only appear at runtime.

If HelpStack is to be used, a configuration file must be placed at `simplified-app-simplye/src/main/assets/helpstack.conf`.

For Zendesk, you should use the following configuration:

```
helpstack.gear = zendesk
helpstack.zendesk.instance_url = ...
helpstack.zendesk.staff_email = ...
helpstack.zendesk.api_token = ...
```

For Salesforce Desk, use the following instead:

```
helpstack.gear = desk
helpstack.desk.instance_url = ...
helpstack.desk.to_help_email = ...
helpstack.desk.staff_login_email = ...
helpstack.desk.staff_login_password =  ...
```

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
