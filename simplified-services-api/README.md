org.librarysimplified.services.api
===

The `org.librarysimplified.services.api` module provides an API for
managing and retrieving application services. It essentially provides
a _pull-based_ dependency injection method where code must explicitly
ask for implementations of interfaces from a service directory. This
is in contrast to _push-based_ dependency injection which typically uses
annotations to have dependencies injected into private class fields.
