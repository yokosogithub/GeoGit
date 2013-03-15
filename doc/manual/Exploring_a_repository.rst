Exploring a repository
=======================

The content of a geogit repository is stored in the ``.geogit`` folder. Unlike a version control system like git, the content of the current working tree cannot be explored directly as a normal folder containing files, since there is not an equivalence between features and files, and they are stored instead in a database which hold the complete structure of the repository.

Exploring the content of a geogit repository is done using geogit commands that allow to list and describe the different elements it contains. The main command used are this task are the following ones:

- ``ls-tree``: lists the content of a tree
- ``cat``: prints a raw text version of an element of the repository
- ``show``: prints a formatted version of an element of the repository


The ls-tree command has the following syntax:

::

	geogit ls-tree  <[refspec]:[path]> [--verbose] [--abbrev] [-t] [-d] [-r]


The ``[refspec]:[path]`` parameter defines the path to be listed. If it contains no refspec part, it will list the given path in the current working tree. that is, ``mypath`` has the same effect as ``WORK_HEAD:mypath``. To list the same path in a different reference, a refspec can be provided. For instance, to list the content of ``mypath`` in the current HEAD, the following line should be used:

::
	
	$geogit ls-tree HEAD:mypath

The provided reference and path should define an element that can eventually resolved to a tree.  That does not include features. Since features do not contains other elements, their content cannot be listed with the ``ls-tree`` command, but with the ``cat`` and ``show`` commands.

An object Id can be used directly instead of refspec and path. That allows to used, for instance, the Id of a commit and list the content of the tree corresponding to that commit.

The options available for the ``ls-tree`` command control how the list of element under the given path is printed. Below you can find examples of the results obtained by using different options.

.. todo:: add examples


Two commands are available to describe an element in a repository: ``cat`` and ``show``. Both of them can be used to describe any type of objects, so the object doesn't have to resolve to a tree, as in the case of the ``ls-tree`` command.

The ``cat`` command prints an unformatted text version of a given element. This text can be used to serialize the element, since it contains all the information needed to store it. This kind of representation is used by some other geogit commands, such as the ones used to create patches.

The ``show`` command prints a formatted description of a given element. This description is a human-readable version of the element, which does not contain all the information needed to serialize it. 

Both commands just take as input a string that defines the object to describe. All supported notations are allowed for both commands. 

Below you can find the output of the ``cat`` and ``show`` command for certain types of objects.

As it can be seen in the following example, when the specified string points to a tree, the ``cat`` command output includes a listing of features or other trees under it. The ``show`` command summarizes that information and also prints the definition of the default feature type associated with that tree.
::

	$geogit show parks
	TREE ID:  0bbed3603377adfbd3b32afce4d36c2c2e59d9d4
	SIZE:  50
	NUMBER Of SUBTREES:  0
	DEFAULT FEATURE TYPE ID:  6350a6955b124119850f5a6906f70dc02ebb31c9

	DEFAULT FEATURE TYPE ATTRIBUTES
	--------------------------------
	agency: <class java.lang.String>
	area: <class java.lang.Double>
	len: <class java.lang.Double>
	name: <class java.lang.String>
	number_fac: <class java.lang.Long>
	owner: <class java.lang.String>
	parktype: <class java.lang.String>
	the_geom: <class com.vividsolutions.jts.geom.MultiPolygon>
	usage: <class java.lang.String>

	$geogit cat parks 
	TREE
	id    0bbed3603377adfbd3b32afce4d36c2c2e59d9d4
	size    50
	numtrees    0
	REF    FEATURE    parks.34    38cadc88ef6dad9f38871d704523ee77f69a7f1d    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.86117933535783;-122.854350067846;42.31833119598368;42.32102693871578;EPSG:4326
	REF    FEATURE    parks.13    b734bc70a8061966e15502c7a0399df61b884dc4    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.86880014388446;-122.86561021610196;42.34400227832745;42.34567119406094;EPSG:4326
	REF    FEATURE    parks.42    eef727418a6cd64960eee0a4e54325e284174218    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.85186496040123;-122.85030419922936;42.3158100546772;42.317125842793224;EPSG:4326
	.
	.
	.


	$geogit cat parks/parks.1



	$geogit cat parks/parks.1  

In the case of specifying a single feature, the output of the ``cat`` command contains just the feature data, while the ``show`` command also prints the names of the corresponding fields, taken from the associated feature type.

the following example shows the output for a commit reference.

::

	$geogit show  509a481257c5791f50f5a35087e432247f9dc8b7
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Test

::

	$geogit cat  509a481257c5791f50f5a35087e432247f9dc8b7
	COMMIT
	id    509a481257c5791f50f5a35087e432247f9dc8b7
	tree    6bc0644ba38372860254c61a62009448ebd8c1e0
	parents    8c08469ffc54f6cc9132855f0415c79cf3fc7785
	author    volaya    volaya@opengeo.org    1358773135891    3600000
	committer    volaya    volaya@opengeo.org    1358773135891    3600000
	message    Test



