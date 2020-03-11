### Releasing

We currently push releases to [Maven Central](https://search.maven.org).

We use the `git flow` model for development and that includes making
releases. The release process essentially involves creating a temporary
release branch from `develop`, incrementing version numbers, merging that
release branch into `master`, pushing binaries to Maven Central, and then
setting the version number for the next development cycle.

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
deployed/released very quickly.

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

#### Finish And Merge The Release Branch

```
$ git flow release finish
```

You will be prompted to add a commit message for the commit that
merges all of the changes back to the `master` branch, and you will
also be prompted to add a message to the new `v99.0.0` tag that `git flow`
will create in the repository. We recommend adding changelog entries
here.

The `git flow` tool will also make sure to clean up any `release`
branch that you may have pushed in the previous step.

#### Push Branches And Tags

```
$ git push --tags
$ git push --all
```

This updates the remote Git repository with the new branches.

#### Push To Maven Central

Assuming that you haven't already, place your Maven Central
credentials in your `$HOME/.gradle/gradle.properties` file. Our projects
use the properties `mavenCentralUsername` and `mavenCentralPassword`
to refer to your username and password, respectively:

```
$ grep mavenCentral $HOME/.gradle/gradle.properties
mavenCentralUsername=hypotheticaluser
mavenCentralPassword=arathersecurepassword
```

With the credentials in place, the `publish`, `closeRepository`, and
`releaseRepository` Gradle tasks can be used to publish artifacts to
Central:

```
$ ./gradlew clean assemble
$ ./gradlew publish
$ ./gradlew closeRepository
$ ./gradlew releaseRepository
```

As closing and releasing a repository can take some time to complete,
it's sometimes reassuring to be able to observe the state of the staging
process. In the `Staging Repositories` tab in the [Sonatype web interface](https://oss.sonatype.org),
you will be able to see output such as this:

![Sonatype staging](./src/site/resources/stages.png?raw=true)

When the _release_ phase completes successfully, artifacts will be
permanently visible on Maven Central within 15 minutes. The artifacts
will appear in [search results](https://search.maven.org) within an hour.

#### Prepare For The Next Development Cycle

Update the `gradle.properties` file to set a new `-SNAPSHOT` version
for the next development cycle:

```
$ $EDITOR gradle.properties
<... edit VERSION_NAME ...>

$ grep VERSION_NAME gradle.properties
VERSION_NAME=99.0.1-SNAPSHOT

$ git add gradle.properties
$ git commit -m 'Start new development cycle; mark version 99.0.1-SNAPSHOT'
```
