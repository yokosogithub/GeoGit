.. _exporting:

Exporting from a GeoGit repository
===================================

Data can also be exported from the GeoGit repository, allowing full synchronization with external applications that cannot use the native format of the GeoGit working tree. 
This also allows to export changes that have been incorporated into the working tree from an external repository, making then available to applications, and making them aware of edits done remotely.

GeoGit supports the same formats for exporting than it does for importing. That is, shapefiles, PostGIS databases and Spatialite. To export from a GeoGit repository, the following syntax is used

::

	$geogit <shp|pg|sl> export <path_to_export> <destination> [-overwrite]


The ``destination`` option is the filepath in the case of exporting to a shapefile, or the table name in case of exporting to a database. In both cases, the element designated by the ``destination`` parameter should not exist. If it exists, GeoGit will not perform the export operation. If you want GeoGit to overwrite, you must explicitly tell it to do so, by using the ``--overwrite`` option.

The ``path_to_export`` refers by default to the working tree. Thus, the path ``roads`` refers to the full reference ``WORK_HEAD:roads``. Data can be exported from a given commit or a different reference, by using a full reference instead of just a path. For instance, the following line will export the ``roads`` path from the current HEAD of the repository, to a shapefile.

::

	$geogit shp export HEAD:roads exported.shp

When exporting to a database, the same options used to configure the database connection that are available for the import operation are also available for exporting.

Notice that, as it was mentioned before, features with different feature types can coexist under the same path. When exporting, this will cause an exception to be thrown, since this is not allowed to happen in a shapefile or a PostGIS table. Only paths with all features sharing the same feature type can be safely imported using the corresponding export commands.