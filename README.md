Simplified
==========

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

Then, simply run:

```
$ mvn clean package
```

### DRM (NYPL and licensees only!)

If the application is to be built with support for Adobe DRM, the
the Adobe-provided `ReaderClientCert.sig` files must be placed in
`src/main/assets` for each of the current application frontends.
The build will check for the existence of these files and fail if
they do not exist. Additionally, the NYPL Adobe DRM package(s) must
be deployed to a local repository.

Once this is done, building the package with Adobe DRM support
enabled is achieved by:

```
$ mvn -P nypl-drm-adobe clean package
```

## Branding And Configurable Features

See [simplified-app-shared/README-Branding.md](simplified-app-shared/README-Branding.md)
for documentation on how to produce your own branded and configured
application.

# Development

This project is developed using the
[git-flow](http://nvie.com/posts/a-successful-git-branching-model/)
branching model.

The [gitflow AVH edition](https://github.com/petervanderdoes/gitflow/)
tool is used to facilitate this. The `master` branch represents the
current production-ready code, and development occurs on the `develop`
branch. All commits and tags are PGP-signed for stronger assurances
of authenticity.

# License

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

