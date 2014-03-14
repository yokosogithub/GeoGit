
.. _geogit-init:

geogit-init documentation
#########################



SYNOPSIS
********
geogit init [<directory>] [--config <config_param> <config_value> [<config_param> <config_value> ...]] 


DESCRIPTION
***********

This command creates an empty `geogit` repository - basically a ``.geogit`` directory with subdirectories for the object, refs, index, and config databases. An initial HEAD that references the HEAD of the master branch is also created.


OPTIONS
*******

--config  <config_param>=<config_value>[,<config_param>=<config_value>...] 		Sets a configuration parameter used to init the repository. Configuration parameters mostly refer to the storage options for the differents elements of the repository. The following parameters are available:

	- storage.graph : sets the storage to use for the graph database. Valid values are 'tinkergraph', 'mongodb',  'neo4j, and 'sqlite''
	- storage.objects : sets the storage to use for the objects database.  Valid values are 'bdbje', 'mongodb', and 'sqlite'
	- storage.staging : sets the storage to use for the staging database. Valid values are 'bdbje', 'mongodb', and 'sqlite'
	- storage.refs : sets the storage to use for references. Currently the only supported value is 'file'

When specifying a given storage for a parameter, a version must also be supplied with another param/value pair. The following parameters and values are supported.
	
	- bdbje.version 0.1
	- tinkergraph.version 0.1
	- mongodb.version 0.1
	- neo4j.version 0.1
	- file.version 1.0
        - sqlite.version 0.1

The mongodb storage backend accepts additional parameters "mongodb.uri" and "mongodb.database" to specify connection parameters for the MongoDB server.

SEE ALSO
********

BUGS
****

Discussion is still open.

