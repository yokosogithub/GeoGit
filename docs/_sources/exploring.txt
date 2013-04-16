Exploring a repository
=======================

The content of a GeoGit repository is stored in the ``.geogit`` folder. Unlike a version control system like *git*, the content of the current working tree cannot be explored directly as a normal folder containing files, since there is not an equivalence between features and files, and they are stored instead in a database which hold the complete structure of the repository.

Exploring the content of a GeoGit repository is done using GeoGit commands that allow to list and describe the different elements it contains. The main command used for this task are the following ones:

- ``ls-tree``: lists the content of a tree
- ``cat``: prints a raw text version of an element of the repository
- ``show``: prints a formatted version of an element of the repository


The ls-tree command has the following syntax:

::

	geogit ls-tree  <[ref]:[path]> [--verbose] [--abbrev num_digits] [-t] [-d] [-r]


The ``[ref]:[path]`` parameter defines the path to be listed. If it contains no ``ref`` part, it will list the given path in the current working tree. that is, ``mypath`` has the same effect as ``WORK_HEAD:mypath``. To list the same path in a different reference, a full reference can be provided. For instance, to list the content of ``parks`` in the current HEAD, the following line should be used:

::
	
	$geogit ls-tree HEAD:parks

The provided reference and path should define an element that can eventually be resolved to a tree.  That does not include features. Since features do not contains other elements, their content cannot be listed with the ``ls-tree`` command, but with the ``cat`` and ``show`` commands.

An object ID can be used directly instead of a ``[ref]:[path]``. That allows to use, for instance, the ID of a commit and list the content of the tree corresponding to that commit.

The options available for the ``ls-tree`` command control how the list of elements under the given path is printed. Using the ``-v`` option will list not just the name of the object that are found, but also its ID and the ID of the corresponding feature type. The ``-r`` option causes the command to list also the content of subtrees recursively. The names of these subtrees are not shown in the listing, but you can tell GeoGit to add them by using the ``-t`` option along with ``-d``. Finally, if you want to show ID's in their abbreviated form, you can use the ``-a`` option followed by the number of digits to show. 

Below you can find examples of the results obtained by using different options, to illustrate the above.


::

	$geogit ls-tree -v parks
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature d8cc931603603bd64506880dc1760b372808ef2d parks.2
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature 8761a24800cdc1c11b6c3c1483a8c9069f657f1f parks.3
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature ff51bfc2a36d02a3a51d72eef3e7f44de9c4e231 parks.1

	$geogit ls-tree -v -r
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature d8cc931603603bd64506880dc1760b372808ef2d parks/parks.2
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature 8761a24800cdc1c11b6c3c1483a8c9069f657f1f parks/parks.3
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature ff51bfc2a36d02a3a51d72eef3e7f44de9c4e231 parks/parks.1

	$geogit ls-tree -v -r -t
	6350a6955b124119850f5a6906f70dc02ebb31c9 tree ad21258c0bade71a879180daf156e1ad1a0c3279 parks
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature d8cc931603603bd64506880dc1760b372808ef2d parks/parks.2
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature 8761a24800cdc1c11b6c3c1483a8c9069f657f1f parks/parks.3
	6350a6955b124119850f5a6906f70dc02ebb31c9 feature ff51bfc2a36d02a3a51d72eef3e7f44de9c4e231 parks/parks.1

	$geogit ls-tree -r -t
	parks
	parks/parks.2
	parks/parks.3
	parks/parks.1

	$geogit ls-tree -v -r -t -a 7
	6350a69 tree ad21258 parks
	6350a69 feature d8cc931 parks/parks.2
	6350a69 feature 8761a24 parks/parks.3
	6350a69 feature ff51bfc parks/parks.1


Two commands are available to describe an element in a repository: ``cat`` and ``show``. Both of them can be used to describe any type of objects, so the object doesn't have to resolve to a tree, as in the case of the ``ls-tree`` command. Trees can also be describe using ``cat`` ad ``show``, but their description does not include just the listing of the elements under it, but also other properties of the tree, as we will see.

The ``cat`` command prints an unformatted text version of a given element. This text can be used to serialize the element, since it contains all the information needed to store it. This kind of representation is used by some other GeoGit commands, such as the ones used to create patches.

The ``show`` command prints a formatted description of a given element. This description is a human-readable version of the element, which does not contain all the information needed to serialize it. 

Both commands just take as input a string that defines the object to describe. All supported notations are allowed for both commands, as they are described in `Referencing a GeoGit element`_.

Below you can find the output of the ``cat`` and ``show`` command for certain types of objects.

As it can be seen in the following first example, when the specified string points to a tree, the ``cat`` command output includes a listing of features or other trees under it. The ``show`` command summarizes that information and also prints the definition of the default feature type associated with that tree.

::

	$geogit show parks
	TREE ID:  0bbed3603377adfbd3b32afce4d36c2c2e59d9d4
	SIZE:  50
	NUMBER Of SUBTREES:  0
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

	$geogit cat parks 
	id    0bbed3603377adfbd3b32afce4d36c2c2e59d9d4
	TREE	
	size    50
	numtrees    0
	REF    FEATURE    parks.34    38cadc88ef6dad9f38871d704523ee77f69a7f1d    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.86117933535783;-122.854350067846;42.31833119598368;42.32102693871578;EPSG:4326
	REF    FEATURE    parks.13    b734bc70a8061966e15502c7a0399df61b884dc4    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.86880014388446;-122.86561021610196;42.34400227832745;42.34567119406094;EPSG:4326
	REF    FEATURE    parks.42    eef727418a6cd64960eee0a4e54325e284174218    6350a6955b124119850f5a6906f70dc02ebb31c9    -122.85186496040123;-122.85030419922936;42.3158100546772;42.317125842793224;EPSG:4326
	.
	.
	.

You can see that the ``cat`` object includes the bounding box and SRS of the feature, and also the IDs corresponding to the feature itself and its feature type.


In the case of specifying a single feature, the output of the ``cat`` command contains just the feature data, while the ``show`` command also prints the names of the corresponding fields, taken from the associated feature type.

::

	$geogit cat HEAD:parks/parks.1
	id    ff51bfc2a36d02a3a51d72eef3e7f44de9c4e231
	FEATURE
	STRING    Medford School District
	DOUBLE    636382.400857
	DOUBLE    3818.6667552
	STRING    Abraham Lincoln Elementary
	LONG    4
	STRING    Medford School District
	STRING    School Field
	MULTIPOLYGON    MULTIPOLYGON (((-122.83646412838807 42.36016644633764, -122.83706843181271 42.36018038487805, -122.83740062537728 42.360187694790284, -122.83773129525122 42.36019528458837, -122.83795404148778 42.36020136945975, -122.83819236923999 42.36020660256662, -122.83846546872873 42.360518040102995, -122.83876233613934 42.36084768643743, -122.83979986790222 42.361999744796655, -122.83876583032126 42.36206395843249, -122.8387666181915 42.36241475445113, -122.8350544594257 42.362400655348836, -122.83505311158638 42.36190072779918, -122.8352814492704 42.36189781560542, -122.83546514962634 42.36183970799634, -122.8355995051357 42.361675638841625, -122.83649163970789 42.36166473464665, -122.83646412838807 42.36016644633764)))
	java.lang.String    Public


	$geogit show HEAD:parks/parks.1

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


Finally, the following example shows the output of both commands for a commit reference.

::

	$geogit show 509a481257c5791f50f5a35087e432247f9dc8b7
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Updated geometry

::

	$geogit cat 509a481257c5791f50f5a35087e432247f9dc8b7
	id    509a481257c5791f50f5a35087e432247f9dc8b7
	COMMIT	
	tree    6bc0644ba38372860254c61a62009448ebd8c1e0
	parents    8c08469ffc54f6cc9132855f0415c79cf3fc7785
	author    volaya    volaya@opengeo.org    1358773135891    3600000
	committer    volaya    volaya@opengeo.org    1358773135891    3600000
	message    Updated geometry

You can check that, as we mentioned in the `Understanding the structure of a GeoGit repository`_ section, the ``HEAD`` reference points to the latest commit, by describing both ``HEAD`` and the ID of the latest commit. You can use the ``log`` command to get the ID of the latest commit. Both descriptions should be identical.

::
	
	$geogit show 509a481257c5791f50f5a35087e432247f9dc8b7
	Commit:        509a481257c5791f50f5a35087e432247f9dc8b7
	Author:        volaya <volaya@opengeo.org>
	Committer:     volaya <volaya@opengeo.org>
	Author date:   (3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Committer date:(3 hours ago) Mon Jan 21 13:58:55 CET 2013
	Subject:       Updated geometry

	$geogit show HEAD
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

