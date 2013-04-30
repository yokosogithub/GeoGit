.. _exploring:

Exploring a repository
=======================

The content of a GeoGit repository is stored in the ``.geogit`` folder. Unlike a version control system like Git, the content of the current working tree cannot be explored directly as a normal folder containing files, since there is not an equivalence between features and files, and they are stored instead in a database which holds the complete structure of the repository.

Exploring the content of a GeoGit repository is done using GeoGit commands that allow to list and describe the different elements it contains. The main commands used for this task are the following ones:

- ``ls``: lists the content of a tree
- ``show``: prints a formatted version of an element of the repository


The ls command has the following syntax:

::

	geogit ls  <[ref]:[path]> [--verbose] [--abbrev num_digits] [-t] [-d] [-r]


The ``[ref]:[path]`` parameter defines the path to be listed. If it contains no ``ref`` part, it will list the given path in the current working tree. that is, ``mypath`` has the same effect as ``WORK_HEAD:mypath``. To list the same path in a different reference, a full reference can be provided. For instance, to list the content of ``parks`` in the current HEAD, the following line should be used:

::
	
	$ geogit ls-tree HEAD:parks

The provided reference and path should define an element that can eventually be resolved to a tree.  That does not include features. Since features do not contains other elements, their content cannot be listed with the ``ls-tree`` command, but with the ``cat`` and ``show`` commands.

An object ID can be used directly instead of a ``[ref]:[path]``. That allows to use, for instance, the ID of a commit and list the content of the tree corresponding to that commit.

The options available for the ``ls-tree`` command control how the list of elements under the given path is printed. Using the ``-v`` option will list not just the name of the object that are found, but also its ID and the ID of the corresponding feature type. The ``-r`` option causes the command to list also the content of subtrees recursively. The names of these subtrees are not shown in the listing, but you can tell GeoGit to add them by using the ``-t`` option along with ``-d``. Finally, if you want to show ID's in their abbreviated form, you can use the ``-a`` option followed by the number of digits to show. 

Below you can find examples of the results obtained by using different options, to illustrate the above.


::

	$geogit -v -r -t
	Root tree/
	    parks/ 
	        parks.2 
	        parks.3 
	        parks.4 
	        parks.1 

	$geogit ls -v parks
	parks/
	    parks.2 49852c03b8dd3c93fcbda7137abda9ad53a9311a bfd1d4bb75e0a4419243ef0ba9d6e9793d31cdab
	    parks.3 49852c03b8dd3c93fcbda7137abda9ad53a9311a 84150cc07326358ac70777d4141a8cfdd8038323
	    parks.4 49852c03b8dd3c93fcbda7137abda9ad53a9311a 5347d1b1b5d828f83e4065e227dcb848b4371637
	    parks.1 49852c03b8dd3c93fcbda7137abda9ad53a9311a ce3e836bcb64f1b647e3dc9dd97700c584063533

	$geogit ls -v -r
	Root tree/
        parks.2 49852c03b8dd3c93fcbda7137abda9ad53a9311a bfd1d4bb75e0a4419243ef0ba9d6e9793d31cdab
        parks.3 49852c03b8dd3c93fcbda7137abda9ad53a9311a 84150cc07326358ac70777d4141a8cfdd8038323
        parks.4 49852c03b8dd3c93fcbda7137abda9ad53a9311a 5347d1b1b5d828f83e4065e227dcb848b4371637
        parks.1 49852c03b8dd3c93fcbda7137abda9ad53a9311a ce3e836bcb64f1b647e3dc9dd97700c584063533

    $geogit ls -v -r -t
	Root tree/
	    parks/ 49852c03b8dd3c93fcbda7137abda9ad53a9311a 224f0086bc4e9b116e7b60dbc414e1cc8d829839
	        parks.2 49852c03b8dd3c93fcbda7137abda9ad53a9311a bfd1d4bb75e0a4419243ef0ba9d6e9793d31cdab
	        parks.3 49852c03b8dd3c93fcbda7137abda9ad53a9311a 84150cc07326358ac70777d4141a8cfdd8038323
	        parks.4 49852c03b8dd3c93fcbda7137abda9ad53a9311a 5347d1b1b5d828f83e4065e227dcb848b4371637
	        parks.1 49852c03b8dd3c93fcbda7137abda9ad53a9311a ce3e836bcb64f1b647e3dc9dd97700c584063533


	$geogit ls -v -r -t -a 7
	Root tree/
	    parks/ 49852c0 224f008
	        parks.2 49852c0 bfd1d4b
	        parks.3 49852c0 84150cc
	        parks.4 49852c0 5347d1b
	        parks.1 49852c0 ce3e836


Describing an element in a GeoGit repository is done using the ``show`` command. It can be used to describe any type of objects, so the object doesn't have to resolve to a tree, as in the case of the ``ls`` command. Trees can also be describe using ``show``, but their description does not include just the listing of the elements under it, but mainly properties of the tree, as we will see.

The ``show`` command prints a formatted description of a given element. This description is a human-readable version of the element, which does not contain all the information needed to serialize it. 

The ``show`` command  just take as input a string that defines the object to describe. All supported notations are allowed for both commands, as they are described in :ref:`referencing`_.

Below you can find the output of the  ``show`` command for certain types of objects.

As it can be seen in the following first example, when the specified string points to a tree, the ``show`` command summarizes that information and also prints the definition of the default feature type associated with that tree.

::

	$ geogit show parks
	TREE ID:  0bbed3603377adfbd3b32afce4d36c2c2e59d9d4
	SIZE:  50
	NUMBER OF SUBTREES:  0
	DEFAULT FEATURE TYPE ID:  6350a6955b124119850f5a6906f70dc02ebb31c9

	DEFAULT FEATURE TYPE ATTRIBUTES
	--------------------------------
	agency: <STRING>
	area: <DOUBLE>
	len: <DOUBLE>
	name: <STRING>
	number_fac: <Long>
	owner: <STRING>
	parktype: <STRING>
	the_geom: <MULTIPOLYGON>
	usage: <STRING>



In the case of specifying a single feature, the ``show`` command prints the values of all attributes, and their corresponding names taken from the associated feature type.

::
	
	$ geogit show HEAD:parks/parks.1

	ID:  ff51bfc2a36d02a3a51d72eef3e7f44de9c4e231

	ATTRIBUTES
	----------
	agency: Medford School District
	area: 636382.400857
	len: 3818.6667552
	name: Abraham Lincoln Elementary
	number_fac: 4
	owner: Medford School District
	parktype: School Field
	the_geom: MULTIPOLYGON (((-122.83646412838807 42.36016644633764, -122.83706843181271 42.36018038487805, -122.83740062537728 42.360187694790284, -122.83773129525122 42.36019528458837, -122.83795404148778 42.36020136945975, -122.83819236923999 42.36020660256662, -122.83846546872873 42.360518040102995, -122.83876233613934 42.36084768643743, -122.83979986790222 42.361999744796655, -122.83876583032126 42.36206395843249, -122.8387666181915 42.36241475445113, -122.8350544594257 42.362400655348836, -122.83505311158638 42.36190072779918, -122.8352814492704 42.36189781560542, -122.83546514962634 42.36183970799634, -122.8355995051357 42.361675638841625, -122.83649163970789 42.36166473464665, -122.83646412838807 42.36016644633764)))
	usage: Public


Finally, the following example shows the output of the ``show`` command for the case of a commit reference

::

	$ geogit show 509a481257c5791f50f5a35087e432247f9dc8b7
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Updated geometry


You can check that, as we mentioned in the :ref:`structure`_ section, the ``HEAD`` reference points to the latest commit, by describing both ``HEAD`` and the Id of the latest commit. You can use the ``log`` command to get the Id of the latest commit. Both descriptions should be identical.

::
	
	$ geogit show 509a481257c5791f50f5a35087e432247f9dc8b7
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Updated geometry

	$ geogit show HEAD
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Updated geometry


Globbing
---------

Some commands in GeoGit, such as the ones used to describe objects shown above, support using wildcards. This way, you can more easily select a set of objects without having to type the name of each of them.

GeoGit uses an ant-like globbing notation, supporting the most common wildcards, namely ``*``, ``?`` and ``**``. The ``*`` character can be any string of any length (including zero characters), while ``?`` represents a single character. The ``**`` string is used to indicate any path, so it will cause the command to recursively search into a given path. For instance, the string ``roads/**/???`` will return all features with a name of just three characters, in any path under ``roads``. that includes ``roads/N501``, and also ``roads/spain/madrid/N501``

Please, check the `section about directory-based tasks in the ant manual <http://ant.apache.org/manual/dirtasks.html>`_ for more information.

Since objects are not stored in the filesystem, but in the repository database, the expansion of wildcards is not (and should not be) performed by the command-line interpreter, but by the GeoGit interpreter itself.

