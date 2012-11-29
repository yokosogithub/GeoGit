
.. _geogit-pg-export:

geogit-pg-export documentation
###############################



SYNOPSIS
********
geogit pg export [options] <feature_type> <table>


DESCRIPTION
***********

This command export a feature type into a table in a PostGIS database. If the table does not exist in the specified database, the 

OPTIONS
*******    


--host          Machine name or IP address to connect to. Default: localhost

--port          Port number to connect to.  Default: 5432

--schema        The database schema to access.  Default: public

--database      The databse to connect to.  Default: database

--user          User name.  Default: postgres

--password      Password.  Default: <no password>

SEE ALSO
********

:ref:`geogit-pg-list`

BUGS
****

Discussion is still open.

