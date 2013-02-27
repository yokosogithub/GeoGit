Understanding the structure of a GeoGit repository
===================================================

This section introduces the different types of elements that a GeoGit repository, so as to give you a better understanding of how it is structured and helping you work more efficiently.

A GeoGit repository can contain the following elements:

- Features. A feature represents a geographical element, and it contains a given set of attributes and at least a geometry. A feature is the equivalent to a shape in a shape file, or a single record in a spatial database.

- Trees. A tree represent a subdivision, it is used to group features with a certain criteria. It might be seen as the equivalent of a shapefile or a table in a database  Trees can be nested (a tree within a tree), so they might also serve as folders.

- Commits. A commit is generated each time some data is written (*committed*) from the staging area into the repository database. A commit points to the data after that commit (it points to a tree under which the data is found), so it represents a given version of the data. You can go back to you data as it was at a certain point, by going to the corresponding commit. 

All these objects are stored in GeoGit and referenced by an ID, so you can use that ID to refer to the object anytime and in any of the commands available in GeoGit when that is needed. The ID is a 40 character string. Here are some valid IDs corresponding to a few GeoGit objects


All IDs are unique, so each of them is reference to just one single object.

.. note:: In GeoGit, even in the working tree, objects are hashed and stored like that.

We will use object IDs frequently in the following section, as we describe the different commands used to work on a GeoGit repository.

Another element that is found in a GeoGit repository is a *ref*. A *ref* is a short string that references a given element in the GeoGit repository, which can be an object of any of the types described above. Think about it as the GeoGit equivalent of a UNIX symbolic link, or a Windows shortcut.

There are three main *refs* that you might use often:

- ``WORK_HEAD``: References the working tree
- ``STAGE_HEAD``: References the staging area
- ``HEAD``: References the current state of the GeoGit repository.

As you see, they just correspond to the three areas in GeoGit. 

Whenever you make a commit and add new data to the GeoGit repository, the ``HEAD`` reference is changed and it will be a reference to the latest commit.

Note: The ``HEAD`` reference can have a different behavior to that, an most of the time it actually points to another reference (it is an indirect reference), but for now, you can think of it as pointing the latest commit in the repository.

Some GeoGit commands create other references as part of the operations they perform. We will see them in detail as we explain the corresponding commands.


About the organization of trees and features
---------------------------------------------

A tree in a GeoGit repository can only contain features or other trees. A feature in a GeoGit repository can be seen as a rough equivalent of a file in a filesystem.

A feature contains a set of attributes, and these attributes must correspond to a given feature type, also stored in the GeoGit repository. For this reason features are stored along with the ID pointing to their feature type.

All trees have an associated feature type, and in the most common case, all features under a tree share the same feature type, particularly the same one of the parent tree. That is, the ID of the feature type for all features under the tree is the same ID that the tree's feature type. However, features with a different feature type can be added to a tree. You can even have a tree with a given feature type and all of its children features having a different feature type. The tree's feature type is considered to be the default type of the features under the tree, but features are not restricted to it.

When we later see how features can be described, we will see that this information about feature types is presented for both features and trees and it can be used for different tasks. It is important to understand that, although that is the most common case, a tree is not the same thing as a feature type. Both trees and features have an associated feature type, and feature types are stored independently.