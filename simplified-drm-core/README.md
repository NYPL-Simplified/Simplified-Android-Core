DRM-Android-Core
=====================

[![Maven Central](https://img.shields.io/maven-central/v/org.librarysimplified.drm/org.librarysimplified.drm.core?style=flat-square)](https://repo1.maven.org/maven2/org/librarysimplified/drm/)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.librarysimplified/org.librarysimplified.drm.core.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/org.librarysimplified/)

![drm](./src/site/resources/drm.jpg?raw=true)

### Usage

Add a dependency on the core API:

```
implementation "org.librarysimplified.drm:org.librarysimplified.drm.core:1.1.0"
```

### Compilation

1. Ensure that the location of your Android SDK is specified in `local.properties`.
   For example:

```
$ cat local.properties
sdk.dir=/path/to/android-sdk
```

2. Build the code:

```
$ ./gradlew clean assemble test
```

3. Optionally publish the code to your local Maven repository:

```
$ ./gradlew clean assemble test publishToMavenLocal
```

4. Optionally publish the code to Maven Central:

```
$ ./gradlew clean assemble test publish
```

### Project Structure

|Module|Description|
|------|-----------|
| [org.librarysimplified.drm.core](https://github.com/NYPL-Simplified/DRM-Android-Core/tree/develop/org.librarysimplified.drm.core) | Core API
| [org.librarysimplified.drm.core.tests](https://github.com/NYPL-Simplified/DRM-Android-Core/tree/develop/org.librarysimplified.drm.core.tests) | Unit tests

### License

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
