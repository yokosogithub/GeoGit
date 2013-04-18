.. _updating:

Updating the repository data. 
==============================

If you have modified your original data, you should update the data in the GeoGit repository, so it is kept synchronized. This section extends what we have already seen about importing data, by considering some special situations that might arise when importing into a repository that already has some previous data. Along with the next section, which covers the GeoGit commands used for exporting data from the GeoGit repository to a file or database, they describe all the tools and commands needed to set a bidirectional communication between a GeoGit repository and external files and databases. These files and databases keep a snapshot of the repository accessible to external applications such as a desktop GIS, which can work on them and modify them.

Let's assume that our GeoGit repository already has some data, which has been imported from a shapefile. That puts the data in the GeoGit working tree, and we have after that staged and committed it to the repository database. Now we have changed the original shapefile adding some new features and editing the old ones, and we want that to be reflected in the GeoGit repository, creating a new version of the data it stores. The way to do this is re-importing the shapefile, running the ``geogit shp import`` command as we did in the first import.

Importing without additional options actually removes the previous tree at the path you specify (or, if you specify no path, the path taken from the data source, into which where feature are to be imported), and then adds those features from the data source. Remember that this is happening only in the working tree, your previous data is safely stored in the repository database. 

However, you might want to perform a different modification of the working tree, such as adding a set of new features from a different shapefile or table. An import without additional options will cause the previous data to be removed, but in this case, we do not want that to happen, since we want both the previous data, and the newly imported one to be merged in the working tree. In that case, and to perform a safe import and add new features without removing he previous ones, the ``--add`` option has to be used. Only additions will be reflected in the working tree. This option is to be used basically when you want to extend the data under a given tree, adding extra features.


Combining different feature types
-----------------------------------

We have assumed that the new features to import have the same feature type as the ones already in the import path, but, as we know, features under the same path do not have to necessarily share the same feature type. In the case of shapefiles, several shapefiles containing features with different feature types can be imported to the same path. In the case of importing from a database, several tables can be imported into the same path in the GeoGit repository.

Imagine that you already imported a shapefile containing polygons with a given set of attributes, and now you want to import into the same path another shapefile with polygons, which contain the same attributes but with an extra one containing the area of each polygon. In that case, feature types do not match, and the situation is different, but GeoGit provide tools to solve it. Apart from the ``--add`` option, an additional option is available to control how the import command should behave when importing features with a feature type different to the default feature type of the specified path: ``--alter``. 

If none of these modifiers is used, GeoGit will import features overwriting the features that already existed. In that case, unmatching feature types are not a problem, since we are just going to remove the previous ones and replace them with the content of the new shapefile. 

If ``--add`` is used, features are imported, regardless of their feature type. The default feature type remains unchanged and the tree will contain features of several different feature types.

If ``--alter`` is used, the feature type of the features to import will be set as the new default feature type of the destination tree. All the features that already existed in it will be modified to match that feature type. After this type of import operation, all features in the destination path will have the path's default feature type. If the new imported features have extra attributes, the features already in the repository will have null values for those fields. If attributes in the features already in the repository do not exist in the new imported attributes, they will be deleted. 

Here is an example to help you understand the above ideas. Let's assume we have a tree named ``points`` with one single feature, with the following attributes:

- ``xcoord: 25``
- ``ycoord: 30``
- ``elevation: 332.3``

Now we import a shapefile into that tree, which contains just another point with the following attributes:

- ``xcoord: 55``
- ``ycoord: 10``
- ``name: point.1``

Using no options, you will have a tree with just one feature, namely the one in the shapefile you have just imported.

Using the ``--add`` option, you will end up having both elements in your tree. That is,

- ``xcoord: 25``
- ``ycoord: 30``
- ``elevation: 332.3``


- ``xcoord: 55``
- ``ycoord: 10``
- ``name: point.1``

No element is changed, and the default feature also remains unchanged and set to the feature type of the first feature.

Using the ``--alter`` option instead, you will end up with the tree containing the elements shown below

- ``xcoord: 25``
- ``ycoord: 30``
- ``name: NULL``


- ``xcoord: 55``
- ``ycoord: 10``
- ``name: point.1``

The feature that was already in the tree has been changed to adapt to the feature type of the newly imported feature. That feature type is now the default feature type of the tree.

When importing from a database, if the "--all" option is selected and a destination path is supplied, the ``--add`` option is automatically added. Otherwise, importing each table would overwrite the features imported previously, and only features from the last table would appear on the selected path after importing. The ``--alter`` and ``--add`` options cannot be used simultaneously