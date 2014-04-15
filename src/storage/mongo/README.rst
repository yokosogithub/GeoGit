Mongo Storage for GeoGit
========================

This module provides support for storing the GeoGit object graph in a `MongoDB
<http://mongodb.com/>` server.

Enabling
--------

Currently GeoGit can only be made to use the Mongo backend by providing an
altered Guice configuration that overrides the default storage modules with the
Mongo ones.  See the TestModule class in the test sources for an example.

Configuration
-------------

The MongoDB store searches for two configuration options:

  * mongo.host - the hostname or IP address where the MongoDB server is.
  * mongo.port - the network port that MongoDB listens on.

Testing
-------

Since the tests for the Mongo backends connect to a Mongo server, they are
**DISABLED** by default to allow building GeoGit without first installing
Mongo.  If you would like to run the tests, enable them by activating the
'mongoOnlineTests' profile in Maven::

  $ mvn test -f src/parent/pom.xml -pl :geogit-mongodb -PmongoOnlineTests

Otherwise, the tests are compiled but not executed as part of the build
process.  By default the tests look for Mongo running on ``localhost`` at port
``27017``; if you would like to connect to Mongo at a different address you can
provide settings in ``~/.geogit-mongo-tests.properties``
