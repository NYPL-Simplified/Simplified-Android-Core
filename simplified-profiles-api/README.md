org.librarysimplified.profiles.api
===

The `org.librarysimplified.profiles.api` module provides APIs and
classes related to _user profiles_.

_User profiles_ are a feature that was contributed by Library For All
for use in their classroom-oriented builds of the Library Simplified
application. A _user profile_ aggregates a set of _accounts_, and stores
per-user preferences and other attributes. Note that _user profiles_
are completely unrelated to the existing concept of [patron user profiles](https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol),
so care should be taken not to confuse the two.

The _user profile_ system is _always_ enabled, but may be run in either
_anonymous_ or _named profile_ mode. In _anonymous_ mode, a user using
the application is using an always-active, unnamed user profile and
all options to switch profiles, configure profiles, create/delete profiles,
are hidden. In _named profile_ mode, the user is first presented with
the option to select a profile. The primary benefit to having the two
modes is that application code can simply refer to "the current profile"
for storing preferences and other data, and can remain agnostic as
to whether or not it is running in a build of the application that
presents full named user profiles, or one that essentially pretends that
profiles do not exist.

NYPL [SimplyE](https://github.com/NYPL-Simplified/Simplified-Android-SimplyE)
builds always run in _anonymous profile_ mode. [Library For All](https://libraryforall.org.au/)
builds always run in _named profile_ mode.
