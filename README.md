# cludje

A clojure web framework. Because the world needs another web framework...

## Usage

At your own risk =) As usual, the documentation is the worst part. Sorry.

Cludje centres around the concepts of 'actions' and 'molds'.

Actions are functions that get run by a user - they're pretty much
the same as controller methods in MVC, except they're not tied to 
a controller object. There's an action DSL for writing these actions
easily - it provides nice access to the request, database, mailer, etc.

Molds are data structures for business objects. You can specify the
names and types of fields, and then Cludje provides a bunch of 
functions for parsing, display, validation, and so on.

There's also some stuff for security and so on.

## License

Copyright © 2013 Brian DeJong

Distributed under the Eclipse Public License, the same as Clojure.
