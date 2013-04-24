GeoGit Quickstart 
==================

GeoGit is a Distributed Version Control System (DVCS) for geospatial data.

This document is a short introduction to the main ideas and elements of GeoGit. It describes how to set up and use GeoGit to version spatial data, introducing the following operations

* Importing unversioned spatial data into GeoGit
* Making changes and storing snapshots or "commits"
* Maintaining independent lines of modifications ("branches")
* Integrating changes from separate branches ("merge")
* Flagging and resolving conflicting edits.
* Synchronizing data across a network ("push" and "pull")
* Mark specific versions of the data as approved or endorsed for use ("tag")
* Exporting data from GeoGit to common spatial formats such as Shapefile.

Installation
-------------

Download `Version 0.1.0 <http://sourceforge.net/projects/geogit/files/geogit-0.1.0/geogit-cli-app-0.1.0.zip/download>`_ from SourceForge. Full documentation is available for download and online.

To install unzip the *geogit-cli-app-0.1.0.zip* to an applications directory, and then add the unzipped geogit/bin/ folder to your ``PATH``.

To test that GeoGit is ready to be used, open a console and type ``geogit help``. You should see a list of available commands like the one shown below.

::

	usage: geogit <command> [<args>]

	The most commonly used geogit commands are:
	--help          Print this help message, or provide a command name to get help for
	add             Add features to the staging area
	apply           Apply a patch to the current working tree
	branch          List, create, or delete branches
	cat             Provide content of an element in the repository
	checkout        Checkout a branch or paths to the working tree
	cherry-pick     Apply the changes introduced by existing commits
	clean           Deletes untracked features from working tree
	clone           Clone a repository into a new directory
	commit          Record staged changes to the repository
	config          Get and set repository or global options
	conflicts       Shows existing conflicts
	diff            Show changes between commits, commit and working tree, etc
	[...]


Configuration
--------------

GeoGit needs some information about you, since it will use it when you create a new snapshot, to identify its author. Before we start working with geospatial data in GeoGit, you have to provide GeoGit with an user name and email, using the ``config`` command as follows:

::

	$ geogit config --global user.name "myname"
	$ geogit config --gloabl user.email "myemail@address.com"

Replace the above data with your preferred name and email.

Initialization
--------------

Unless you are getting your data from another GeoGit repository (which we will see how to do later in this quickstart), the first thing you have to do is to create your own repository. Create the folder that will contain the repository, move into it, and initialize the GeoGit repository typing 

::

	$ geogit init

Now your GeoGit repository is ready to hold your geospatial data and manage different versions of it.

Importing data
---------------

To version a dataset, it has to be imported into the repository. Data in different formats can be imported into GeoGit. 

Download the data in `this zip file <https://github.com/opengeo/GeoGit/blob/0.1.0/doc/quickstart/quickstart_data.zip?raw=true>`_. It contains several folders, each of them with a snapshot of the dataset. We will use them to create our GeoGit repository, simulating the edition and creation of all the versions it contains. 

We will start by importing the ``snapshot1/parks.shp`` shapefile, using the following command:

::

	$ geogit shp import snapshot1/parks.shp
	Importing from shapefile snapshot1\parks.shp
	Importing parks            (1/1)... 100%
	snapshot1\parks.shp imported successfully.

The command above assumes you have unzipped the data file in the folder where you have created your GeoGit repo (don't worry, it will not collide with the GeoGit repo itself, since that is all stored in the ``.geogit`` folder that the ``init`` command has created). If you have unzipped the data somewhere else, adjust the above path accordingly.

The data from the shapefile is now in the so-called *working-tree*. This means it is not versioned yet, but it is already in a format that GeoGit can understand, so it can be aware of the data and the changes you might introduce.

Run the following command to see that your data is actually in the working tree.

::

	$ geogit ls-tree -r
	parks/parks.2
	parks/parks.3
	parks/parks.1

As you can see, features from the shapefile are added to the working tree under a tree named ``parks``. The name is taken from the filename of the shapefile. A tree in a GeoGit repository can be seen as the equivalent of a folder in a filesystem

Running the ``status`` command will give you information about the data you have that is not already versioned.

::

	$ geogit status
	# On branch master
	# Changes not staged for commit:
	#   (use "geogit add <path/to/fid>..." to update what will be committed
	#   (use "geogit checkout -- <path/to/fid>..." to discard changes in working directory
	#
	#      added  parks/parks.2
	#      added  parks/parks.3
	#      added  parks/parks.1
	# 3 total.


Adding data
-----------

To tell GeoGit that you want to version the data in the working tree, you have to *add* it. To do it, run the following command.

::

	$ geogit add
	Counting unstaged features...3
	Staging changes...
	100%
	3 features staged for commit
	0 features not staged for commit

Now your data is ready to be used to create a new snapshot (a *commit* in the GeoGit terminology).

If you now run the ``status``, command, you will see a different output, since your data has been added and it is now versioned. 

::

	$ geogit status
	# On branch master
	# Changes to be committed:
	#   (use "geogit reset HEAD <path/to/fid>..." to unstage)
	#
	#      added  parks/parks.2
	#      added  parks/parks.3
	#      added  parks/parks.1
	# 3 total.
	#

When your data is added, it is copied onto the so-called *staging area*, which is the last area before it actually gets written to the repository database to create a new version.

Committing
-----------

Commiting means creating a new version with the data currently in the staging area. You have imported your data and then added it, so now the staging area contains exactly the same data as your shapefile. By committing it, you will crate a new snapshot containing that data.

Type the following command:

::

	$ geogit commit -m "first version"
	100%
	[592006f6b541557a203279be7b4a127fb9dbb2d9] first version
	Committed, counting objects...3 features added, 0 changed, 0 deleted.

The text between quotes after the ``-m`` option is the commit message, which identifies and describes the snapshot that you create.

Adding a new version
---------------------

You can add a new version, by importing new data, adding it and then commiting it. GeoGit does not incorporate tools to edit your data, which has to be done externally. GeoGit only takes care of versioning it. 

The ``snapshot2/parks.shp`` file provided with the example data has the same data as the first file we imported, but with an extra feature.

If you run the ``status`` command after importing (and before adding), you will see it reports 1 added element. GeoGit can recognise the changes that have been done and identify the differences, and will not report modifications in the feature that haven't been changed.

::

	$ geogit status
	# On branch master
	# Changes not staged for commit:
	#   (use "geogit add <path/to/fid>..." to update what will be committed
	#   (use "geogit checkout -- <path/to/fid>..." to discard changes in working directory
	#
	#      added  parks/parks.4

	# 4 total.


Add the new feature and commit to create a new version

::

	$ geogit add
	Counting unstaged features...1
	Staging changes...
	100%
	1 features staged for commit
	0 features not staged for commit

	$ geogit commit -m "first modification"
	100%
	[7b6e36db759da8d09b5b1bb726009b3d2c5ca5f7] first modification
	Committed, counting objects...1 features added, 0 changed, 0 deleted.

Showing the history of the repository
--------------------------------------

You can use the ``log`` command to see the history of your repository. The history is basically a collection of commits, ordered in reverse chronological order (most recent first)

::

	$ geogit log
	Commit:  7b6e36db759da8d09b5b1bb726009b3d2c5ca5f7
	Author:  volaya <volaya@opengeo.org>
	Date:    (19 minutes ago) 2013-04-11 15:24:10 +0300
	Subject: first modification

	Commit:  592006f6b541557a203279be7b4a127fb9dbb2d9
	Author:  volaya <volaya@opengeo.org>
	Date:    (25 minutes ago) 2013-04-11 15:18:14 +0300
	Subject: first version


Creating a branch
-----------------

Data editing can be done on the main history line of the repository, but also on additional ones, so the main line can be kept clean and safe while you perform those edits. This also allows you to create 'what if' scenarios without altering the data in your repository, which might be being used by other. Once your edits are finished and you think it's worth adding them to the main history, you can merge them, as we will soon see.

To create a new branch named *myedits*, run the following command.

::
	$ geogit branch myedits -c
	Created branch refs/heads/myedits

The ``-c`` option tells GeoGit to switch your repository to that branch. Everything you do now will be added to this new history line, not the main one, as it was the case before.

Use the ``snapshot3/parks.shp`` to create a new snapshot (once again, import it, add it and then commit it). It contains the same data of the last version, but with a new extra feature. 

The ``log`` command will now show you a history like the one shown below:

::

	$ geogit log

	Commit:  c04d0a968696744bdc32bf865f9675a2e55bf447
	Author:  volaya <volaya@opengeo.org>
	Date:    (26 minutes ago) 2013-04-11 15:27:15 +0300
	Subject: added new feature

	Commit:  7b6e36db759da8d09b5b1bb726009b3d2c5ca5f7
	Author:  volaya <volaya@opengeo.org>
	Date:    (29 minutes ago) 2013-04-11 15:24:10 +0300
	Subject: first modification

	Commit:  592006f6b541557a203279be7b4a127fb9dbb2d9
	Author:  volaya <volaya@opengeo.org>
	Date:    (35 minutes ago) 2013-04-11 15:18:14 +0300
	Subject: first version


Merging changes from a different branch
----------------------------------------

You can merge changes from a different branch into your current branch. Our repository has now two branches: the one we have created (*myedits*) and the main history one. The main history branch is always named *master*.

Let's move the changes we have just added from the *myedits* branch into the *master* branch.

First move to the branch where you want to move changes to, in this case *master*. The ``checkout`` command, followed by the name of the branch, will make that branch the current active one.

::

	$ geogit checkout master
	Switched to branch 'master'

The ``log`` command will now show the following history (we are producing a less verbose version of the history, by adding the ``--oneline`` option):

::
	
	$ geogit log --oneline
	7b6e36db759da8d09b5b1bb726009b3d2c5ca5f7 first modification
	592006f6b541557a203279be7b4a127fb9dbb2d9 first version


The last commit is missing since it was added to the *myedits* branch. The *master* branch remains unchanged.

To merge the work done in the *myedits* branch into the current *master* branch, enter the following command:

::

	$ geogit merge myedits
	100%
	[c04d0a968696744bdc32bf865f9675a2e55bf447] added new feature
	Committed, counting objects...1 features added, 0 changed, 0 deleted.


Now the commit introduced in the branch is already present in the main history, as the log operation will tell you.

::

	$ geogit log	--oneline
	c04d0a968696744bdc32bf865f9675a2e55bf447 added new feature
	7b6e36db759da8d09b5b1bb726009b3d2c5ca5f7 first modification
	592006f6b541557a203279be7b4a127fb9dbb2d9 first version


Handling merge conflicts
-------------------------

In the above case, the work done on the branch could be added without problems, but it is not always like that.

Let's do the following: create a new branch named "fix", and create a commit based in the ``snapshot4/parks.data`` shapefile. This new shapefile corrects a geometry, and it updates the corresponding area field to reflect that change. Use the ``checkout`` command to go back to *master*, and there create a new commit with the data in ``snapshot5/parks.data``. This is the same data as ``snapshot3/parks.data``, but changes the units in the *area* field.

What we have now is a conflict case, since the original version (the one corresponding to our ``snapshot3/parks.data`` file), has been changed differently in two branches, *master* and *fix*, as both have altered the *area* field.

If you now try to merge, GeoGit cannot automatically resolve that merge, since you have made changes in both branches, and they are incompatible (you can't have the two new attribute values, but just one). The output of the ``merge`` command will be like this:

::

	$ geogit merge fix
	100%
	CONFLICT: Merge conflict in parks/parks.5
	Automatic merge failed. Fix conflicts and then commit the result.

You can see that there is a conflict by running the ``status`` command

::

	$ geogit status
	# On branch master
	# Changes to be committed:
	#   (use "geogit reset HEAD <path/to/fid>..." to unstage)
	#
	#      modified  parks/parks.2
	#      modified  parks/parks.3
	#      modified  parks/parks.4
	#      modified  parks/parks.1
	# 4 total.
	#
	# Unmerged paths:
	#   (use "geogit add/rm <path/to/fid>..." as appropriate to mark resolution
	#
	#      unmerged  parks/parks.5
	# 1 total.

An unmerged path represents a conflicted element

You can get more details about the conflict by running the ``conflicts`` command

::
	$ geogit conflicts --diff
	---parks/parks.5---
	Ours
	area: 15297.503295898438 -> 15246.59765625

	Theirs
	area: 15297.503295898438 -> 164594.90384123762

The output indicates that the value in the *area* attribute of the *parks.5* feature is causing the conflict.

The conflict has to be solved manually, and you have to merge both versions yourself, or just select one of them to be used, discarding the other.

[NOTE: once we have a UI, we should change this to show a manual merge using the UI]

Let's assume we want to use the changed feature in the branch, not the one in *master*. Run the following command.

::

	$ geogit checkout --theirs

That puts the branch version in the working tree, overwriting the previous one. Add it and that will remove the conflict.

::

	$ geogit add

And now commit it. There is no need to add a commit message, since that is created automatically when you are in a merge operation

::

	$ geogit commit


Tagging a version
------------------

You can add a tag to a version, to easily identify it with something more descriptive than the ID associated to each commit.

To do so, use the ``tag`` command like this:

::

	$ geogit tag "First official version"

Now you can refer to the current version with that name.

Exporting from a GeoGit repository
-----------------------------------

Data can be exported from a GeoGit repository into several formats, ready to be used by external applications

To export a given tree to a shapefile, use the ``shp export`` command.

::

	$ geogit shp export parks parks.shp

That will create a file named ``parks.shp`` with the content of the ``parks`` tree.

Past versions can be exported by prefixing the tree name with a commit ID and a colon, like in the following example.

::

	$ geogit shp export HEAD~1:parks parks.shp

HEAD~1 refers to the previous commit, not the one corresponding to the last version we created, so this will export the example tree as it was just before the last commit.



Synchronizing GeoGit repositories
---------------------------------

A GeoGit repository can interact with other GeoGit repositories (known as *remotes*) that version the same data, getting changes from them or adding its own changes to them.

Also, an existing repository can be cloned, so you do not start with an empty one, as in the case of using the ``init`` command. 

Let's clone the repository we have been working on until now. Create a new empty folder in your filesystem, move into it and run the following command (replace the path with the current path were you had your GeoGit repository)

::

	$ geogit clone /path/to/repo

Now you can start working on this new repository as usual, and you changes will be put on top of the changes that already exist in there, which were cloned from the original repository.

You can bring changes from the so-called ``origin`` repository, by using the ``pull`` command

::

	$ geogit pull origin

This will update the current branch with changes that have been made on that branch in the remote repository since the last time both repositories were synchronized.

To move your changes from your repository and into the remote ``origin`` one, you can use the ``push`` command

::

	$ geogit push
