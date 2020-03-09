org.librarysimplified.presentableerror.api
===

The `org.librarysimplified.presentableerror.api` module provides a set
of classes for working with _presentable_ values. A value is said to be
_presentable_ the code producing the value has done the necessary work
to ensure that any strings are correctly localized. The concept of
presentability essentially exists in order to give application code the
ability to decide, for example, whether it is safe to display an error 
message directly to the user or to instead display a generic error 
message. Without the ability to see if an error is _presentable_, the
application would always have to err on the side of safety and present
generic error messages (to the detriment of usability and the ability
to debug issues).
