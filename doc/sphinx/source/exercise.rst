A complete exercise
====================

The following is a complete exercise that uses most of the commands explained in this document, presenting a typical GeoGit workflow. The necessary data can be found in this zip file, which contains some shapefiles representing different versions of the same sample dataset. The commands used assume that you have unzipped its content in your ``~/geogit_data`` folder. If you have it somewhere else, adapt the corresponding paths accordingly.

The exercise will create several GeoGit repositories and, by following it, you will play the role of different agents working each of them in its own repository, and all of them coordinated through a central one.

The first thing to start working is to create a GeoGit repository, import some data, stage it and commit. That will be our central repository. We will use the ``parks_1.shp`` file, and it will be stored in the ``~/geogit_repos/repo_a`` folder. Create that folder and run the following commands (if you want to put your ``geogit_repos`` folder in a different location, feel free to do it, but do not forget to change all references when running the examples in this exercise).

::
	$geogit init

	$geogit shp import ~/geogit_data/parks_1.shp

	geogit add

	geogit commit -m "First commit"


Let's check that the data is in the working tree and that it has also been committed, by listing the content of the working tree and the tree corresponding to this first commit.

::

	$geogit ls-tree


We will now leave that repository and will not work directly on it anymore, since that is the central repository. Instead, create a folder named ``repo_a`` and clone the central repository in there. That will represent the repository of an external collaborator.

::

	$cd repo_a
	$geogit clone ~/geogit_repos/central_repo

The repository is not empty, but contains the same history and data as the original one that we have cloned. So we can work on its data and then push the changes we make back into the original repository, in case we want to change them.

Now let's make some changes to our working tree. The data it contains (the data that we cloned from the central repository) it is just one tree named ``parks`` with 3 geometries. We are going to add one new feature, with its corresponding polygon and attributes. You can export the working tree to a shapefile, open it in your favorite GIS and edit it, but we have prepared the edited file in the ``parks_2.shp`` file. The only thing that you have to do is to import it

::

	$geogit shp import ~/home/geogit_data/parks_2.shp

And once the new data is in the working tree, stage it and make a new commit

:: 

	$geogit add
	$geogit commit -m "Added new feature"

We have committed the change to the ``master`` branch, since we had not created any new branch. So now our ``master`` branch is different from the master branch of the original repository. To synchronize both of them and add the work we have done to the original repository, we can use the ``push`` command.

::



Let's now put another player in the game. A new person joins our team of people working on our data. The first thing he has to do is to clone the central repository. Create a folder named ``repo_b`` in your repositories folder and clone it there.

Since we had already pushed the changed made by the first collaborator in his repository (in the ``repo_a`` folder), this second collaborator will get them when he clones the central repository.

Suppose now that this new collaborator starts working on the repository data. Instead of working on the ``master`` branch, he creates a new branch named ``fixes``, in which he plans to correct some wrong data that he has found. Move to the ``repo_b`` folder and run the following command:


::

	$geogit branch fixes -c

The ``parks_3.shp`` shapefile contains a modified version of the data, in which some attributes have been modified and a group of points in a geometry have been moved. Import it into the working tree of the ``rebo_b`` repository, and then stage an commit the changes.

::

	$geogit shp import ~/home/geogit_data/parks_3.shp
	$geogit add
	$geogit commit -m "Corrected minor errors"

Now this commit has been added to the ``fixes`` branch. You can see the differences between this latest version and the latest one (which is to say, the differences between the ``parks_2.shp`` and ``parks_3.shp`` files), by running the ``diff`` command. We want the difference between the current ``HEAD`` and its ancestor, so we can use the following line:

::

	$geogit diff HEAD HEAD~1

You can see that one of the changes is to correct an entry in the *area* attribute, which holds the area of the polygon.

Considering that this modification is finished, the second collaborator merges the ``fixes`` branch into ``master``

::

	$geogit checkout master
	$geogit merge fixes

and pushes the changes to the central repository

::

	$geogit

The first collaborator also has started working on the data in his repository. He has also created a new branch named ``cleanup``, in which he wants to remove some data. He has removed some redundant points in a couple of polygons, and also he has changed the values of a the *area* attribute, so now they are expressed in square kilometers instead of square miles. 

The corresponding modified data can be found on the ``parks_4.shp``
