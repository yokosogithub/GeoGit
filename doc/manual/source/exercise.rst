A complete exercise
====================

The following is a complete exercise that uses most of the commands explained in this document, presenting a typical GeoGit workflow. The necessary data can be found in this zip file, which contains some shapefiles representing different versions of the same sample dataset. The commands used assume that you have unzipped its content in your ``~/geogit_data`` folder. If you have it somewhere else, adapt the corresponding paths accordingly.

The exercise will create several GeoGit repositories and, by following it, you will play the role of different agents working each of them in its own repository, and all of them coordinated through a central one.

The first thing to start working is to create a GeoGit repository, import some data, stage it and commit. That will be our central repository. We will use the ``parks_1.shp`` file, and it will be stored in the ``~/geogit_repos/central_repo`` folder. Create that folder and run the following commands (if you want to put your ``geogit_repos`` folder in a different location, feel free to do it, but do not forget to change all references when running the examples in this exercise).

::

	$geogit init
	Initialized empty Geogit repository in ~/geogit_repos/central_repo
	Importing from shapefile ~/geogit_data/parks_1.shp
	Importing parks            (1/1)... 100%
	~/geogit_data/parks_1.shp imported successfully.
	$geogit add
	Counting unstaged features...4
	Staging changes...
	100%
	4 features staged for commit
	0 features not staged for commit
	100%
	$geogit commit -m "First commit"
	[b4ffc26570362cbf913d79e243e821dad22b2bf0] First commit
	Committed, counting objects...4 features added, 0 changed, 0 deleted.


Let's check that the data is in the working tree and that it has also been committed, by listing the content of the working tree and the tree corresponding to this first commit.

::

	$geogit ls-tree


We will now leave that repository and will not work directly on it anymore, since that is the central repository. Instead, create a folder named ``repo_a`` and clone the central repository in there. That will represent the repository of an external collaborator.

::

	$cd repo_a
	$geogit clone ~/geogit_repos/central_repo

The repository is not empty, but contains the same history and data as the original one that we have cloned. So we can work on its data and then push the changes we make back into the original repository, in case we want to change them.

Now let's make some changes to our working tree. The data it contains (the data that we cloned from the central repository) it is just one tree named ``parks`` with 4 geometries. We are going to add one new feature, with its corresponding polygon and attributes. You can export the working tree to a shapefile, open it in your favorite GIS and edit it, but we have prepared the edited file in the ``parks_2.shp`` file. The only thing that you have to do is to import it.	

::

	$geogit shp import ~/home/geogit_data/parks_2.shp parks
	Importing from shapefile ~/geogit_data/parks_2.shp
	Importing parks            (1/1)... 100%
	~/geogit_data/parks_2.shp imported successfully.


And once the new data is in the working tree, stage it and make a new commit.

:: 

	$geogit add		
	Counting unstaged features...5
	Staging changes...
	100%
	5 features staged for commit
	0 features not staged for commit
	100%
	$geogit commit -m "Added feature"
	[4d761f56d61e21f06e6a0ede54654377053738ee] added feature
	Committed, counting objects...1 features added, 0 changed, 0 deleted.
	

We have committed the change to the ``master`` branch, since we had not created any new branch. So now our ``master`` branch is different from the ``master`` branch of the original repository. To synchronize both of them and add the work we have done to the original repository, we can use the ``push`` command.

::

	$git push origin master

Let's now put another player in the game. A new person joins our team of people working on our data. The first thing he has to do is to clone the central repository. Create a folder named ``repo_b`` in your repositories folder and clone it there.

Since we had already pushed the changes made by the first collaborator in his repository (in the ``repo_a`` folder), this second collaborator will get them when he clones the central repository.

Suppose now that this new collaborator starts working on the repository data. Instead of working on the ``master`` branch, he creates a new branch named ``fixes``, in which he plans to correct some wrong data that he has found. Move to the ``repo_b`` folder and run the following command:


::

	$geogit branch fixes -c
	Created branch refs/heads/fixes

The ``parks_3.shp`` shapefile contains a modified version of the data, in which an attribute has been modified and a point in a geometry has been moved. Import it into the working tree of the ``rebo_b`` repository, and then stage and commit the changes.

::

	$geogit shp import ~/home/geogit_data/parks_3.shp parks
	Importing from shapefile ~/home/geogit_data/parks_3.shp
	Importing parks            (1/1)... 100%
	~/home/geogit_data/parks_3.shp imported successfully.
	$geogit add
	Counting unstaged features...1
	Staging changes...
	100%
	1 features staged for commit
	0 features not staged for commit
	$geogit commit -m "fixed minor error"
	100%
	[ef0e5369e1dc90939b5e110232236c90cfa448f4] fixed minor error
	Committed, counting objects...0 features added, 1 changed, 0 deleted.

Now this commit has been added to the ``fixes`` branch. You can see the differences between this last version and the latest one (which is to say, the differences between the ``parks_2.shp`` and ``parks_3.shp`` files), by running the ``diff`` command. We want the difference between the current ``HEAD`` and its ancestor, so we can use the following line:

::

	$geogit diff HEAD HEAD~1
	49852c... 49852c... 14ca94... 9e3da2...   M  parks/parks.5
	the_geom: MultiPolygon -122.8559991285487,42.3325881068491 -122.85599502570052,42.33258714736789 -122.8555527064439,42.332583529914544 -122.8555547256435,42.332720688578576 -122.8555550824813,42.332745029854586 -122.85509985857445,42.332745552581905 -122.85499037285732,42.33264794481705 -122.85494140418146,42.332648464841405 -122.85480580923854,42.33213439963994 -122.85481284656451,42.33122907675051 -122.85553321700381,42.33122736814138 [-122.8559877370252,42.33122815590696] (-122.8559029952351,42.331228422464314) -122.85598889427041,42.33135716537447 -122.85599263311514,42.33177278813245 -122.85599750841081,42.33231457227876 -122.85599997275685,42.33258811539997 -122.8559991285487,42.3325881068491
	area: 15297.503295898438 -> 15246.59765625

You can see that one of the changes is to correct an entry in the *area* attribute, which holds the area of the polygon. This has been recalculated, since the geometry has changed after correcting the position of a point.

Considering that this modification is finished, the second collaborator merges the ``fixes`` branch into ``master``

::

	$geogit checkout master
	$geogit merge fixes
	100%
	[ef0e5369e1dc90939b5e110232236c90cfa448f4] fixed minor error
	Committed, counting objects...0 features added, 1 changed, 0 deleted.

and pushes the changes to the central repository

::

	$geogit push origin

The first collaborator also has started working on the data in his repository. He has also created a new branch named ``cleanup``, in which he wants to remove some data. He has removed some redundant points in a couple of polygons, and also he has changed the values of a the *area* attribute, so now they are expressed in square feet instead of square meters. 

The corresponding modified data can be found on the ``parks_4.shp``, and the work described above can be replicated with the lines below:

::

	$cd repo_a
	$geogit branch -c cleanup
	Created branch refs/heads/cleanup
	$geogit shp import ~/home/geogit_data/parks_4.shp parks
	$geogit add
	$geogit commit -m "Removed redundant data and changed units area"

Now the first collaborator wants to merge his changes into the ``master`` branch, but first he fetches changes from the master ``branch`` in the central repository, in case someone else has done some extra work. He does so by running the pull command.

::

	$git pull origin master	

By doing so, he receives the changes introduced by the second collaborator, and one extra commit is added to the master branch of his local repository. Now it is time to merge. Checkout the ``master`` branch and merge by running the commands below:

::

	$geogit checkout master
	$geogit merge cleanup
	100%
	CONFLICT: Merge conflict in parks/parks.5
	Automatic merge failed. Fix conflicts and then commit the result.

As you can see, the merge operation did not go as expected. The changes introduced by the second collaborator, which are now in master after being pulled from the central repository, are not compatible with the changes in the ``cleanup`` branch. Some features are not affected, since they have not been modified by both histories, but in other cases, there is no way GeoGit can automatically perform a merge. The conflicts have to be edited manually.

To see why those conflicts exist and the changes introduced in each branch, you can do the following.