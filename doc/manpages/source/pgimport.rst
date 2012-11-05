
.. _geogit-pg-import:

geogit-pg-import documentation
#########################



SYNOPSIS
********
geogit pg import [options] [--all|-t <table>]


DESCRIPTION
***********

This command imports one or more tables from a PostGIS database into the GeoGit working tree.  Either ``-t`` or ``--all`` must be set for the import process to commence.  To see a list of available tables, use ``geogit pg import``.

OPTIONS
*******    

-t, --table     The table to import.

--all           Import all tables.

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

