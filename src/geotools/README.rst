######################################################
GeoGit GeoTools Extension
######################################################

This extension to GeoGit is meant to provide a means for importing and exporting
geospatial data to and from GeoGit into a variety of data stores.

GeoGit Details
=======

Project Lead: `Gabriel Roldan <https://github.com/groldan>`_

Source files use the following header::
   
   /* Copyright (c) 2013 OpenPlans. All rights reserved.
    * This code is licensed under the Modified BSD license, available at the root
    * application directory.
    */
 
As indicated above the code is distributed under an `Modified BSD <LICENSE.txt>`_ license.

Build
=====

The GeoGit GeoTools extension is built using Maven::
  
  cd src
  mvn clean install

Functional Tests
----------------

Functional tests require a PostGIS database that can be used for testing, and are run with the online profile::

  mvn -Ponline
  
The PostGIS database must be populated with the test tables from the .sql file::

  'src/test/resources/org/geogit/geotools/cli/test/functional/geogit_pg_test.sql'
  
When the functional tests are run for the first time, a database configuration file will be created at the
user root directory called '.geogit-pg-tests.properties'.  Open this file and enter the connection information
for the test database.

Additional Profiles
-------------------
    
Any additional build profiles are documented in the root `pom.xml`:pom.xml .

If you would like to work in Eclipse use of the `m2eclipse plugin <http://www.sonatype.org/m2eclipse>`_ recommended.

Please carefully apply the code formatting options in the buld/eclipse/formatter.xml file. These are the standard
Java formatting options with 100 character line length for both code and comments, and 4 spaces for indentation.
It is also recommended to use the code templates from build/eclipse/codetemlates.xml.

Participation
=============

The project is hosted on github:

* https://github.com/opengeo/GeoGit

Participation is encouraged using the github *fork* and *pull request* workflow::

* file headers are described above
* include test case demonstrating functionality
* contributions are expected to pass test and not break the build

Project resources:

* `Full on-line project documentation <http://opengeo.github.com/GeoGit>`_
* `GeoGit Discussion Group <https://groups.google.com/a/opengeo.org/group/geogit/>`_
* The build is `actively monitored using hudson <http://hudson.opengeo.org/hudson/view/geogit/>`_
* https://github.com/opengeo/GeoGit/issues

Additional resources:

* `guava-libraries <http://code.google.com/p/guava-libraries/>`_

