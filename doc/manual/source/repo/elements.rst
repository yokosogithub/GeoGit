.. _repo.elements:

Repository elements
===================

This section introduces the different types of elements of a GeoGit repository, so as to give you a better understanding of how it the repository is structured.

Standard elements
-----------------

A GeoGit repository can contain the following elements:

* **Features**. A feature represents a geographical element, and it contains a given set of attributes and a geometry. A feature is the equivalent to a shape in a shapefile, or a single record in a database.

* **Trees**. A tree represent a subdivision, it is used to group features with a certain criteria. It is the equivalent of a shapefile or a table in a database. Trees can be nested, so they might also serve as folders.

* **Commits**. A commit is generated each time some data is written ("committed") from the staging area into the repository database. A commit points to the data after that commit (in other words, it points to a tree under which the data is found), so it represents a given snapshot of the data. You can return to how your data was at a certain point by viewing the data at its corresponding commit. 

All these objects are stored in GeoGit and referenced by an ID. You can use that ID to refer to the object anytime and in any of the commands available in GeoGit when that is needed. The ID is a 40 character string. For example, ``509a481257c5791f50f5a35087e432247f9dc8b7`` is a valid ID. All IDs are unique.

.. note:: In GeoGit, even in the working tree, all elements are given an ID.

Object IDs will be used when describing the different commands used to work on a GeoGit repository.

References
----------

Another element found in a GeoGit repository is a **ref**. A ref, or reference, is a short string that references a given element in the GeoGit repository, which can be an object of any of the types described above. Think about it as the GeoGit equivalent of a symbolic link or a shortcut.

There are three main refs that are used:

* ``WORK_HEAD``: References the working tree
* ``STAGE_HEAD``: References the staging area
* ``HEAD``: References the current state of the GeoGit repository.

As you see, they just correspond to the three areas in GeoGit. 

Whenever you make a commit and add new data to the GeoGit repository, the ``HEAD`` reference is changed and it will be a reference to the latest commit.

.. note:: The ``HEAD`` reference is an indirect reference, so it is actually slightly more involved, but for now it can be thought of as referring to the latest commit in the repository.

Some GeoGit commands will create other refs as part of their operations.

Trees, features, and feature types
--------------------------------------------------------------

A feature contains a set of attributes, and these attributes must correspond to a given feature type, also stored in the GeoGit repository. For this reason features are stored along with the ID pointing to their feature type.

A tree in a GeoGit repository can only contain features or other trees. A feature in a GeoGit repository can be seen as a rough equivalent of a file in a file system.

All trees have an associated feature type, and in most cases, all features under a tree share the same feature type. That is to say that the ID of the feature type for all features under the tree is the same ID of the tree's feature type. However, it is possible to add features with a different feature type to a tree. The tree's feature type is considered to be the default type of the features under the tree, but features are not restricted to it.

Although it is the most common case, a tree is not the same as a feature type. Both trees and features have an associated feature type, and feature types are stored independently.
