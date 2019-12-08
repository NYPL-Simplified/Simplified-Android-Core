org.librarysimplified.analytics.api
===

The `org.librarysimplified.analytics.api` module provides an event-based
API for logging analytics data.

The application is expected to publish events via the `AnalyticsType`
interface. Analytics systems wishing to consume the published events
must register implementations of the `AnalyticsSystemProvider` interface
via `ServiceLoader`. Registered implementations will then receive all
published events and can handle them as necessary.

### Privacy

Analytics system providers will receive _all_ analytics events logged
by the application, but individual system providers are responsible for
actually logging or discarding those events. In other words, the set
of _logged_ events is typically a superset of the events _consumed_
by registered analytics system providers, because most system providers
are used in applications that have privacy policies that exclude the
logging of many of the published events.

As a real-life example, the NYPL's SimplyE application has a strong 
privacy policy that only results in _book opened_ events being logged -
all of the other published analytics events are simply discarded.
