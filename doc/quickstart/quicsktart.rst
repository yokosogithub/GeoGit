GeoGit Quickstart 
=======================

GeoGit is a Distributed Version Control System (DVCS) for geospatial data.

This document is a short introduction to the main ideas and elements of GeoGit. It describes how to set up and use GeoGit to version spatial data, introducing the following operations

* Importing unversioned spatial data into GeoGit
* Making changes and storing snapshots or "commits"
* Reverting to a previous snapshot ("checking out" a version)
* Maintaining independent lines of modifications ("branches")
* Integrating changes from separate branches ("merge")
* Flagging and resolving conflicting edits.
* Synchronizing data across a network ("push" and "pull")
* Mark specific versions of the data as approved or endorsed for use ("tag")
* Exporting data from GeoGit to common spatial formats such as Shapefile.

Installation
-------------

[To be written]

Configuration
--------------

GeoGit needs some information about you, since it will use it when you create a new snapshot, to identify its author. Before we start working with geospatial data in GeoGit, you have to provide GeoGit with an user name and email, using the ``config`` command as follows:

::

	$geogit config --global user.name "myname"
	$geogit config --gloabl user.email "myemail@address.com"

Replace the above data with your preferred name and email.

Initialization
--------------

Unless you are getting your data from another GeoGit repository (which we will see how to do later in this quickstart), the first thing you have to do is to create your own repository. Create the folder that will contain the repository, move into it, and initialize the GeoGit repository typing 

::

	$geogit init

Now your GeoGit repository is ready to hold your geospatial data and manage different versions of it.

Importing data
---------------

To version a dataset, it has to be imported into the repository. Data in different formats can be imported into GeoGit. We will import the ``example.shp`` shapefile, using the following command:

::

	$geogit shp import example.shp

The data from the shapefile is now in the so-called *working-tree*. This means it is not versioned yet, but it is already in a format that GeoGit can understand, so it can be aware of the data and the changes you might introduce.

Run the following command to see that your data is actually in the working tree.

::

	$geogit ls-tree

As you can see, features from the shapefile are added to the working tree under a tree named ``example``. The name is taken from the filename of the shapefile. A tree in a GeoGit repository can be seen as the equivalent of a folder in a filesystem


Adding data
-----------

To tell GeoGit that you want to version the data in the working tree, you have to *add* it. To do it, run the following command.

::

	$geogit add

Now your data is ready to be used to create a new snapshot (a *commit* in the GeoGit terminology).

If you now run the ``ls-tree``, command, you will see a different output, since your data has been added and it is now versioned. When your data is added, it is copied onto the so-called *staging area*, which is the last area before it actually gets written to the repository database to create a new version.

Committing
-----------

Commiting means creating a new version with the data currently in the staging area. You have imported your data and then added it, so now the staging area contains exactly the same data as your shapefile. By committing it, you will crate a new snapshot containing that data.

Type the following command:

::

	$geogit commit -m "first version"

The text between quotes after the ``-m`` option is the commit message, which identifies and describes the snapshot that you create.

Adding a new version
---------------------

You can add a new version, by importing new data, adding it and then commiting it. GeoGit does not incorporate tools to edit your data, which has to be done externally. GeoGit only takes care of versioning it. 

Open the shapefile in an application that allows you to edit it (we recommend you QGIS), and modify one of the features. For instance, you can change an attribute value and move some of the points in the geometry of that feature. Then save the shapefile and follow the same steps that you did already.

If you run the ``ls-tree`` command after importing (and before adding), you will see it reports just one changed element. GeoGit can see that, althought you have imported the whole shapefile, only one of the features has been changed.

Add the changed feature and commit to create a new version

::

	$geogit add
	$geogit commit -m "first modification"


Reverting to a previous version
--------------------------------

[NOTE: What is the use of that?? In git, you checkout a version to work on it, but here you have to export it to actually be able do work on the data. Exporting can be done providing any valid commit reference, so there is no need to revert the working tree to recover a version and work on it... Maybe this section should not be here in a quickstart guide, but just in a more advanced manual where reverting to a previous version might be more useful
Also, checking out a version creates a detached head state, which is too advanced for a quickstart, I guess]

After commiting, you have two versions of your data in the GeoGit repository. The data in the working tree corresponds to the last version that you have created. However, you can go back to any of the previous versions that you may have stored in your repository

To see a list of all version use the ``log`` command

::
	
	$geogit log

The string at the left of each commit message is an identifier that allows you to make a reference to a given GeoGit element such as a commit. You do not need to use the full length of it. The first 6 characters are usually enough for GeoGit to know what you mean. In the case of the first commit, you can refer to it using ``XXXXXX` as identifier. 

To revert ("checkout") to the version defined by that first commit, type the following in your console:

::

	$geogit checkout XXXXXX

Now your working tree contains the data corresponding to the first version.


Creating a branch
-----------------

Data editing can be done on the main history line of the repository, but also on additional ones, so the main line can be kept clean and safe while you perform those edits. This also allows you to create 'what if' scenarios without altering the data in your repository, which might be being used by other. Once your edits are finished and you think it's worth adding them to the main history, you can merge them, as we will soon see.

To create a new branch named *myedits*, run the following command.

::
	$geogit branch myedits -c

The ``-c`` option tells GeoGit to switch your repository to that branch. Everything you do now will be added to this new history line, not the main one, as it was the case before.

Edit the shapefile once again, this time adding a new feature. Then, import it (the ``ls-tree`` command, if you run it, will report a new feature, with no other features modified), add it, and commit to create a new version.

The ``log`` command will now show you a history like the one shown below:

::

	$geogit log


Merging changes from a different branch
----------------------------------------

You can merge changes from a different branch into your current branch. Our repository has now two branches: the one we have created (*myedits*) and the main history one. The main history branch is always named *master*.

Let's move the changes we have just added from the *myedits* branch into the *master* branch.

First move to the branch where you want to move changes to, in this case *master*. The ``checkout`` command, followed by the name of the branch, will make that branch the current active one.

::

	$geogit checkout master

The ``log`` command will now show the following history:

::
	
	$geogit log


The last commit is missing since it was added to the *myedits* branch. The *master* branch remains unchanged.

To merge the work done in the *myedits* branch into the current *master* branch, enter the following commands:

::

	$geogit merge myedits


Now the commit introduced in the branch is already present in the main history, as the log operation will tell you.

::

	$geogit log


Handling merge conflicts
-------------------------

In the above case, the work done on the branch could be added without problems, but it is not always like that.

Let's do the following: create a new branch, modify a feature, and then commit it, so a new version is added to the branch. Use the ``checkout`` command to go back to *master*, and there, edit the same feature but differently (if you have edited an attribute value, modify the same one, but setting a different value. If you have modified a geometry, alter the same points, but moving them to a different place.). Commit that change.

If you now try to merge, GeoGit cannot automatically resolve that merge, since you have made changes in both branches, and they are incompatible (you can't have the two new attribute values, but just one. And you can't have a point in two different places at the same time). The output of the ``merge`` command will be like this:

::

	$geogit merge

You can see that there is a conflict by running the ``log`` command

::

	$geogit log

And you can get more details about the conflict by running the ``conflicts`` command

::

	$geogit conflicts


The conflict has to be solved manually, and you have to merge both versions yourself, or just select one of them to be used, discarding the other.

[NOTE: once we have a UI, we should change this to show a manual merge using the UI]

Let's assume we want to use the changed feature in the branch, not the one in *master*. Run the following command.

::

	$geogit checkout --theirs

That puts the branch version in the working tree, overwriting the previous one. Add it and that will remove the conflict.

::

	$geogit add

And now commit it. There is no need to add a commit message, since that is created automatically when you are in a merge operation

::

	$geogit commit


Tagging a version
------------------

You can add a tag to a version, to easily identify it with something more descriptive than the ID associated to each commit.

To do so, use the ``tag`` command like this:

::

	$geogit tag "First official version"

Now you can refer to the current version with that name.

Exporting from a GeoGit repository
-----------------------------------

Data can be exported from a GeoGit repository into several formats, ready to be used by external applications

To export a given tree to a shapefile, use the ``shp export`` command.

::

	$geogit shp export example example.shp

That will create a file named ``example.shp`` with the content of the ``example`` tree.

Past versions can be exported by prefixing the tree name with a commit ID and a colon, like in the following example.

::

	$geogit shp export XXXXXX:example example.shp

If you remember, ``XXXXX`` was ID of the first version we created, so this will export the example tree as it was in that first version.



Synchronizing GeoGit repositories
---------------------------------

A GeoGit repository can interact with other GeoGit repositories (known as *remotes*) that version the same data, getting changes from them or adding its own changes to them.

Also, an existing repository can be cloned, so you do not start with an empty one, as in the case of using the ``init`` command. 

Let's clone the repository we have been working on until now. Create a new empty folder in your filesystem, move into it and run the following command (replace the path with the current path were you had your GeoGit repository)

::

	$geogit clone /path/to/repo

Now you can start working on this new repository as usual, and you changes will be put on top of the changes that already exist in there, which were cloned from the original repository.

You can bring changes from the so-called ``origin`` repository, by using the ``pull`` command

::

	$geogit pull origin

This will update the current branch with changes that have been made on that branch in the remote repository since the last time both repositories were synchronized.

To move your changes from your repository and into the remote ``origin`` one, you can use the ``push`` command

::

	$geogit push
