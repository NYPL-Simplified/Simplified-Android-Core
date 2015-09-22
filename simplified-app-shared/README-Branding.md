Branding
========

The application is designed as a reusable and generic core that can
be customized and branded without necessarily needing to make any
changes to source code. The main code and resources are defined in
the module that you are currently looking at, and are packaged as an
Android AAR library. Producing a custom branded application is therefor
simply a case of declaring a dependency on the `simplified-app-shared`
package in your Maven or Gradle build, and then providing your own
image resources and a configuration resource.

The project currently has three predefined branded applications, and
these serve as practical examples of how to produce your own:

  * [simplified-app-openebooks](../simplified-app-openebooks)
  * [simplified-app-vanilla](../simplified-app-vanilla)
  * [simplified-app-simplye](../simplified-app-simplye)

The rest of this documentation assumes that the user has at least
a passing familiarity with either [Maven](http://maven.apache.org)
or [Gradle](http://gradle.org). The process is not tied to either
of these build systems but the user is expected to understand, for
example, what it means to declare a dependency on an AAR package,
and how Android projects are typically organized.

Customization
=============

Firstly, it's necessary to declare a dependency on the
`simplified-app-shared` package. In Maven:

```
<dependency>
  <groupId>org.nypl.simplified</groupId>
  <artifactId>simplified-app-shared</artifactId>
  <version>INSERT_LATEST_VERSION_HERE</version>
  <type>aar</type>
</dependency>
```

The application core is designed to support a few features that
can be configured at build-time. All user configurable values
are exposed in a resource file called `features.xml`, and the
`simplified-app-shared` package provides a default version of
this file. By creating your own `features.xml` and placing it
(by convention) at `src/main/res/values/features.xml` in your own
project, you will essentially override the package-provided file and
the application core will instead use the values you have defined at
build and run-time. Currently, all values are required to be
provided, even if those values are not used by your particular
application (this is due to a limitation in Android).

The file provides the following configuration variables:

|Variable|Type|Description|
|--------|----|-----------|
|`feature_app_name`|`string`|This is the name that will be displayed for the application.|
|`feature_app_icon`|`drawable`|An Android `drawable` resource that will be used for the application icon.|
|`feature_app_splash`|`drawable`|An Android `drawable` resource that will be used for the application splash screen.|
|`feature_main_color`|`color`|An Android `color` that will be used as the primary color in the application's theme.|
|`feature_holds_enabled`|`bool`|If the value of this variable is `true`, the application will display a `Holds` section in the navigation drawer.|
|`feature_catalog_start_uri`|`string`|This is the URI of the feed that will be treated as the root of the catalog.|
|`feature_catalog_loans_uri`|`string`|This is the URI of the user's own book shelf.|
|`feature_adobe_vendor_id`|`string`|This is the Adobe Vendor ID that will be used for Adobe DRM operations, if DRM is enabled.|
|`feature_adobe_package_override`|`string`|If a non-empty string is provided here, this will be used as the package name for Adobe DRM. This is primarily useful during development, as the package name must match the name given in the certificate.|

