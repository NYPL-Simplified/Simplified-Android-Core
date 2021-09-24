### Releasing

We currently push releases to [Maven Central](https://search.maven.org)
and [Firebase](https://firebase.google.com).

We use the `git flow` model for development and that includes making
releases. The release process essentially involves creating a temporary
release branch from `develop`, incrementing version numbers, merging that
release branch into `main`, and then setting the version number for the next 
development cycle.

The instructions in this file detail the process for producing a
hypothetical version `99.0.0` release. The instructions assume that
you will be using the [Git Flow AVH Edition](https://github.com/petervanderdoes/gitflow-avh)
command-line tool. It's possible to perform all of these steps without
it, but there is essentially no benefit to doing so (and many more
opportunities to get things wrong). The instructions also assume that
all Maven Central repository operations will be performed from the
command-line. It is also possible to perform some of them via the
[Sonatype Web Interface](https://oss.sonatype.org), but that is not
covered here.

#### Testing Repository

New developers can try out the process detailed here by cloning the
[Maven-Central-Testing](https://github.com/NYPL-Simplified/Maven-Central-Testing)
project and performing the release instructions below. The `Maven-Central-Testing`
project is configured identically to the `Simplified-Android-Core`
repository, but contains only a single module and can be built and
deployed/released very quickly. This allows developers unfamiliar with
Maven Central to run through the release process without actually having
to do a real release of the application.

#### PGP

Maven Central requires that all deployed artifacts are signed using
[PGP](https://en.wikipedia.org/wiki/Pretty_Good_Privacy). As part of
the quality checks performed on uploaded packages, the Maven Central
repository will attempt to fetch the public key of the package signer
from the [OpenPGP public keyservers](https://sks-keyservers.net/) in
order to verify package signatures. Therefore, the person doing the
signing needs to ensure that their key is accessible on those key
servers.

Generating PGP keys and uploading those keys is outside of the scope of
this documentation. The guide written by [Curtis Wallen](http://curtiswallen.com/pgp/)
may be helpful.

#### Ensure Git Flow Is Configured

If you have not previously run `git flow init` on the repository, do
so now:

```
$ git flow init

Which branch should be used for bringing forth production releases?
   - develop
   - master
Branch name for production releases: [master] 

Which branch should be used for integration of the "next release"?
   - develop
Branch name for "next release" development: [develop] 

How to name your supporting branch prefixes?
Feature branches? [] feature/
Bugfix branches? [] bugfix/
Release branches? [] release/
Hotfix branches? [] hotfix/
Support branches? [] support/
Version tag prefix? [] v
```

#### Create A Release Branch

```
$ git flow release start 99.0.0
```

This creates a new `release/99.0.0` branch to which various commits
may be made to increment version numbers, update change logs, run
any last test builds, etc.

#### Verify Library Dependencies

SimplyE uses several remote repositories as library dependencies published to Maven Central. Verify that each library dependency is referencing the latest version in the `build_libraries.gradle` file. The main libraries to focus on here are:  
[nypl_audiobook_api](https://github.com/NYPL-Simplified/audiobook-android)  
[nypl_drm_adobe](https://github.com/NYPL-Simplified/DRM-Android-Adobe)  
[nypl_drm_axis](https://github.com/NYPL-Simplified/DRM-Android-Axis)  
[nypl_drm_core](https://github.com/NYPL-Simplified/DRM-Android-Core)  
[nypl_findaway](https://github.com/NYPL-Simplified/audiobook-audioengine-android)  
[nypl_http](https://github.com/NYPL-Simplified/Simplified-Android-HTTP)  
[nypl_overdrive](https://github.com/NYPL-Simplified/audiobook-android-overdrive)  
[nypl_pdf](https://github.com/NYPL-Simplified/pdfreader-android)  
[nypl_readium2](https://github.com/NYPL-Simplified/Simplified-R2-Android)

*Note: In some cases, there will be unreleased work in the above library dependencies. In that case it may require doing a Maven Central release for that library dependency order to get that back in the main project. Currently CI will not automatically check for this.*

#### Update Version Numbers

The `gradle.properties` file for the project defines the version number
for all modules. The version number used cannot match any version number
used for any existing release on Maven Central. We attempt to follow
[semantic versioning](https://www.semver.org) as much as we can.

```
$ grep VERSION_NAME gradle.properties
VERSION_NAME=98.0.0

$ $EDITOR gradle.properties
<... edit VERSION_NAME ...>

$ grep VERSION_NAME gradle.properties
VERSION_NAME=99.0.0

$ git add gradle.properties
$ git commit -m 'Mark version 99.0.0'
```

Optionally, you can `git push` here to give continuous integration
systems a chance to build the code and make sure everything is alright.

#### Close The Changelog

We currently use [changelog](https://www.io7m.com/software/changelog/) to
maintain a humanly-readable list of changes made between releases. The
[changelog manual](https://www.io7m.com/software/changelog/documentation/index.xhtml#d2e143)
has a detailed usage guide, but the release process only involves a couple
of commands.

First, [set the release version](https://www.io7m.com/software/changelog/documentation/index.xhtml#id_f79aa94b-4dc7-44ee-823e-f6d1e3e8f155)
to the version being released now. In our case, that's `99.0.0`:

```
$ changelog release-set-version --version 99.0.0
```

Then, [close the current release](https://www.io7m.com/software/changelog/documentation/index.xhtml#id_31fe1fbf-62b9-4811-93dd-252a9ebfb222).
This marks the changelog as being finalized for the current release:

```
$ changelog release-finish
```

```
$ git add README-CHANGES.xml
$ git commit -m 'Close changelog'
```

#### Finish And Merge The Release Branch

```
$ git flow release finish
```

You will be prompted to add a commit message for the commit that
merges all of the changes back to the `master` branch, and you will
also be prompted to add a message to the new `v99.0.0` tag that `git flow`
will create in the repository. We recommend adding changelog entries
here. You can get a plain-text version of the changelog for the current
release that you can copy and paste into the merge message using the
following command:

```
$ changelog write-plain
Release: LibrarySimplified 99.0.0
Change: Enable ACS DRM for Readium 2 (Ticket: #SIMPLY-3138)
Change: Add a neutral age gate for the Play Store (Ticket: #SIMPLY-3493)
Change: Correct an issue related to multiple accounts and Adobe DRM (thanks @ray-lee!) (Ticket: #SIMPLY-2979)
...
```

The `git flow` tool will also make sure to clean up any `release`
branch that you may have pushed in the previous step.

#### Push Branches And Tags

```
$ git push --tags
$ git push --all
```

This updates the remote Git repository with the new branches.

The release is now complete! Our [CI](https://github.com/NYPL-Simplified/Simplified-Android-CI)
system takes care of pushing all of the pieces of the build to the right places.
If you're morbidly curious as to what happens there, see the [epilogue](#epilogue).
To leave things in a pleasant state for the next developer, you should now
[prepare for the next development cycle](#prepare-for-the-next-development-cycle).

#### Prepare For The Next Development Cycle

Make sure you're back on the `develop` branch. The `git flow` tool should have
switched branches for you automatically, but it's always worth being certain:

```
$ git branch
develop
```

Update the `gradle.properties` file to set a new `-SNAPSHOT` version
for the next development cycle:

```
$ $EDITOR gradle.properties
<... edit VERSION_NAME ...>

$ grep VERSION_NAME gradle.properties
VERSION_NAME=99.0.1-SNAPSHOT

$ git add gradle.properties
```

Start a [new changelog release](https://www.io7m.com/software/changelog/documentation/d2e143.xhtml#id_3be05f7b-b312-45f8-ae0c-00a0528a6273)
(assuming the next release will be `99.0.1` - it doesn't matter if you don't
know what the exact version number will be, because this can always be changed
later):

```
$ changelog release-begin 99.0.1
$ git add README-CHANGES.xml
```

Commit:

```
$ git commit -m 'Start new development cycle; mark version 99.0.1-SNAPSHOT'
$ git push
```

## Epilogue

#### How Does Pushing To Maven Central Work?

We currently use the [brooklime](https://www.io7m.com/software/brooklime)
tool to do reliable Maven Central deployments. This documentation
makes references to a `brooklime.jar` file, and this should be understood
to be an abbreviation for whichever is the current release of the `brooklime`
command-line tool. At the time of writing, the exact jar file is
[com.io7m.brooklime.cmdline-0.1.0-main.jar](https://repo1.maven.org/maven2/com/io7m/brooklime/com.io7m.brooklime.cmdline/0.1.0/com.io7m.brooklime.cmdline-0.1.0-main.jar).

We generally rely on CI to do this for us, but it can also be performed
manually by developers on their local machine if necessary. See the
[.ci/ci-deploy-central-release.sh](ci-deploy-central-release.sh) script
to see how the commands described below are used by the CI system to upload
builds.

##### Building And Deploying

First, build the code as normal using `./gradlew clean assemble`.

Then, deploy the artifacts that will be uploaded to Maven Central into
a directory using the `org.librarysimplified.directory.publish` Gradle
property:

```
$ ./gradlew -Porg.librarysimplified.directory.publish="$HOME/tmp/simplified" publishAllPublicationsToDirectoryRepository
```

The above command uses a directory `$HOME/tmp/simplified` to contain
the binaries that will be uploaded to Maven Central for the current
release. You can use whatever directory you like, but you should use
a fresh directory for each release to avoid re-uploading artifacts from
older releases that may have been left hanging around.

##### Creating A Staging Repository

Then, using the `brooklime` tool, create a new staging repository:

```
$ java -jar brooklime.jar \
  create \
  --description 'Simplified X.Y.Z' \
  --stagingProfileId af061f5afba777 \
  --user MyMavenCentralUser \
  --password MyMavenCentralPassword

orglibrarysimplified-1087
```

The above command creates a new staging repository in the project's
staging profile (`af061f5afba777`). The `X.Y.Z` string should be
replaced with the version number of the release you are deploying,
and the `MyMavenCentralUser` and `MyMavenCentralPassword` strings
should be your Maven Central username and password, respectively. The
command will print the name of the new staging repository when
execution completes. In this case, it printed `orglibrarysimplified-1087`,
but the exact value will differ each time a new repository is created.

##### Uploading Content To The Staging Repository

```
$ java -jar brooklime.jar \
  upload \
  --stagingProfileId af061f5afba777 \
  --repository orglibrarysimplified-1087 \
  --user MyMavenCentralUser \
  --password MyMavenCentralPassword \
  --directory "$HOME/tmp/simplified"
```

The above command uploads the contents of the directory that we
populated during the build, to the staging repository `orglibrarysimplified-1087`
that we created in the previous step. The uploading step will typically
upload around two thousand files, and will generally take around ten
minutes to complete.

##### Closing And Releasing The Staging Repository

```
$ java -jar brooklime.jar \
  close \
  --stagingProfileId af061f5afba777 \
  --repository orglibrarysimplified-1087 \
  --user MyMavenCentralUser \
  --password MyMavenCentralPassword
  
$ java -jar brooklime.jar \
  release \
  --stagingProfileId af061f5afba777 \
  --repository orglibrarysimplified-1087 \
  --user MyMavenCentralUser \
  --password MyMavenCentralPassword
```

The two commands above will _close_ and then _release_ the staging
repository `orglibrarysimplified-1087`. This completes the deployment
process.

As closing and releasing a repository can take some time to complete,
it's sometimes reassuring to be able to observe the state of the staging
process. In the `Staging Repositories` tab in the [Sonatype web interface](https://oss.sonatype.org),
you will be able to see output such as this:

![Sonatype staging](./src/site/resources/stages.png?raw=true)

When the _release_ phase completes successfully, artifacts will be
permanently visible on Maven Central within 15 minutes. The artifacts
will appear in [search results](https://search.maven.org) within an hour.

