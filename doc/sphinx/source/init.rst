Creating a GeoGit repository
=============================

The first thing to do to start working with GeoGit is to create a GeoGit repository. A repository keeps your data and all the different versions of it, so you can go back to a given version at any time, and evolve the current data, creating new versions.

GeoGit is a command-line application, and it is run from the console typing commands in the form of ``geogit <command_to_execute> <options>``. To create a new repository, the ``init`` command is used.

::

	$geogit init

This creates a new folder in the current folder, named ``.geogit``. That folder will contain all your data, both the current version and the history of old versions that were created. Other than that folder, GeoGit will put nothing else under the folder from which you called the ``init`` command. You can put whatever you want on that folder, whether spatial or non-spatial data files, since GeoGit will ignore them.

.. note:: In Geogit, the working tree is not the folder where the ``.geogit`` subfolder is located. The working tree itself is also located within the ``geogit`` folder.

The GeoGit repository that we have just created is empty, that meaning that it contains no spatial data at all. Even if the folder where the repository has been created previously contained some files, those are not part of the repository itself, and they are ignored as well.

To start adding data to our repository, we need to import data into its working tree, which we will explain next.