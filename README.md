Simplified
==========

## IDE, Plugins, Versions, etc.

##### JDK 1.8 (min required 1.7) [download](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 

##### Maven 3.3.9 [download](https://maven.apache.org/download.cgi)
add settings.xml to .m2 directory

request nexus credentials on slack [librarysimplified](https://librarysimplified.slack.com/messages/simplified-android/)
 
```
  <?xml version="1.0" encoding="UTF-8"?>
  <settings
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
   <servers>
      <server>
        <id>nypl-nexus-group</id>
        <username>****</username>
        <password>****</password>
      </server>
    </servers>
    <profiles>
      <profile>
        <id>nypl</id>
        //keystore for signing the app, remove this property if you don't have a keystore yet.
        <properties>
            <sign.keystore>****</sign.keystore>
            <sign.alias>nypl</sign.alias>
            <sign.storepass><![CDATA[****]]></sign.storepass>
            <sign.keypass><![CDATA[****]]></sign.keypass>
        </properties>
        <repositories>
          <repository>
            <id>nypl-nexus-group</id>
            <name>NYPL nexus repo group</name>
            <url>https://nexus.librarysimplified.org:8443/nexus/content/groups/external</url>
            <layout>default</layout>
          </repository>
        </repositories>
      </profile>
    </profiles>
    <activeProfiles>
      <activeProfile>nypl</activeProfile>
    </activeProfiles>
  </settings>
```



### Android Studio 

[Download](http://tools.android.com/download/studio/builds/1-5) Version 1.5 of Android Studio. 

##### SDK Manager
Install at least the Android SDK Package 4.4.2
 
##### Other SDK Tools needed: 
```
Android SDK Build Tools
Android SDK Tools
Android SDK Platform Tools
Local Maven repository for Support Libraries
Android Support Library
Google Play Service
Google Play APK Expansion Library
Google Play Licensing Library
Android NDK
```

##### Environment Variables

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_74.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk-bundle
export PATH=$PATH:$ANDROID_HOME/platform-tools/
export PATH=$PATH:$HOME/Library/Maven/bin/
```

![config](https://www.dropbox.com/s/e60sexwzwlmdzg1/androidSdkManager.png?dl=1)


#### Clone this repository or your forked repository


##### Import cloned project

![config](https://www.dropbox.com/s/nn0f7vs20p4s545/1.png?dl=1)
![config](https://www.dropbox.com/s/4ofkxd1teqh7rft/2.png?dl=1)
![config](https://www.dropbox.com/s/8xdz3cqi1w4ooa0/3.png?dl=1)


##### Build Configuration

Select a Module: simplye, openebooks, vanilla...

![config](https://www.dropbox.com/s/gchjcg4d8wjby7m/buildConfiguration.png?dl=1)

Add a Maven goal with the following command. Any other goal which might be added automatically, should be removed.

```
-pl simplified-app-shared,simplified-app-simplye -am clean package -U -P nypl
```

```
-pl simplified-app-shared,simplified-app-openebooks -am clean package -U -P nypl
```

With Adobe DRM (NYPL and licensees only!)

```
-pl simplified-app-shared,simplified-app-simplye -am clean package -U -P nypl,nypl-drm-adobe
```

```
-pl simplified-app-shared,simplified-app-openebooks -am clean package -U -P nypl,nypl-drm-adobe
```


### IntelliJ IDEA 
[Download](https://www.jetbrains.com/idea/#chooseYourEdition), if you prefer using IntelliJ over Android Studio.
After setup with Android Studio, you can use IntelliJ going forward with the following modifications.

##### Build Configuration
Make sure any other goal is removed, only the maven goal should remain.





## Building

#### Without DRM

```
$ mvn clean package
```

#### With DRM (NYPL and licensees only!)

If the application is to be built with support for Adobe DRM, the
the Adobe-provided `ReaderClientCert.sig` files must be placed in
`src/main/assets` for each of the current application frontends.
The build will check for the existence of these files and fail if
they do not exist. 

Once this is done, building the package with Adobe DRM support
enabled is achieved by:

```
$ mvn -P nypl-drm-adobe clean package
```

## HelpStack

If HelpStack is to be used, a configuration file must be provided
in the `assets` directory of the application being built. So, to
enable Zendesk for the `SimplyE` application, create a file at
`simplified-app-simplye/src/main/assets/helpstack.conf` with the
correct URL and credentials:

Configuration for Zendesk:

```
helpstack.gear                 = zendesk
helpstack.zendesk.instance_url = https://nonexistent.zendesk.com
helpstack.zendesk.staff_email  = nobody@example.com
helpstack.zendesk.api_token    = *************
```

Configuration for Desk.com:

```
helpstack.gear                        = desk
helpstack.desk.instance_url           = https://nonexistent.desk.com
helpstack.desk.to_help_email          = nobody@example.com
helpstack.desk.staff_login_email      = nobody@example.com
helpstack.desk.staff_login_password   = *************
```

## Branding And Configurable Features

See [simplified-app-shared/README-Branding.md](simplified-app-shared/README-Branding.md)
for documentation on how to produce your own branded and configured
application.


## License

```
© 2015 The New York Public Library, Astor, Lenox, and Tilden Foundations

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing
permissions and limitations under the License.
```

This product includes code derived from the [Readium Android SDK
Launcher](https://github.com/readium/SDKLauncher-Android), which
is distributed under the following license:

```
Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the organization nor the names of its contributors may be
used to endorse or promote products derived from this software without specific
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE
```
