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

We have commited the change to the ``master`` branch, since we had not created any new branch. So now our ``master`` branch is different from the master branch of the original repository. To synchronize both of them and add the work we have done to the original repository, we can use the ``push`` command.

::



Let's now put another player in the game. A new person joins our team of people working on our data. The first thing he has to do is to clone the central repository. Create a folder named ``repo_b`` in your repositories folder and clone it there.

Since we had already pushed the changed made by the first collaborator in his repository (in the ``repo_a`` folder), this second collaborator will get them when he clones the central repository