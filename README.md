Simplified
==========

## Building

First, deploy the Android SDK artifacts to a repository using
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

## Development

This project is developed using the
[git-flow](http://nvie.com/posts/a-successful-git-branching-model/)
branching model.

The [gitflow AVH edition](https://github.com/petervanderdoes/gitflow/)
tool is used to facilitate this. The `master` branch represents the
current production-ready code, and development occurs on the `develop`
branch. All commits and tags are PGP-signed for stronger assurances
of authenticity.

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
