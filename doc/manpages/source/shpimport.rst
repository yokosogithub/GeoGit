
.. _geogit-shp-import:

geogit-shp-import documentation
################################



SYNOPSIS
********
geogit shp import <shapefile> [<shapefile>]... [--path <path>] [--fid-attrib <attrib_name>] [--add] [--alter]


OPTIONS
********

--path <path>					The path to import to. If not specified, it uses the filename of the shapefile

--fid-attrib <attrib_name>		Uses the specified attribute as the feature id of each feature to import. If not used, a number indicating the position in the shapefile is used

--add							Adds the imported feature to the corresponding tree without removing previous features in case the tree already exists

--alter							Same as the ``--add`` switch, but if the feature type of the imported features is different to that of the destination tree, the default feature type is changed and all previous features are modified to use that feature type




DESCRIPTION
***********

This command imports features from one or more shapefiles into the GeoGit working tree.

BUGS
****

Discussion is still open.

