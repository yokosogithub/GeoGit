Merging branches
=================

When you work on a new branch, eventually you will want to put the data on that branch into the master branch. You might also want to combine the work done on two separate branches. Whatever the case, it's common in a normal GeoGit workflow to merge the content of two or more branches. Graphically, this is what happens to a GeoGit repository after you merge two branches.

.. figure:: merge.png


Merging is usually done when you have finished working on a given branch and you think the modifications that you have made are ready to become part of the main ``master`` branch. In that case, you will apply all the same changes that you have made to the features and trees in a that branch onto the features and trees in the ``master`` branch, and the ``master`` branch will end up having both the changes that were introduced in it since the creation of the branch to merge, and the changes indeendently made in that branch to merge.

Merging is done using the ``geogit merge`` commmand. If you are on the destination branch, then you should just add the name of the branch from which to take the changes that you are going to include in the current branch. If you are in ``master`` and want to add into it the changes from a branch named ``mybranch``, you would use the following command.

::

	geogit merge mybranch

That will add new commits to the current branch, which will include the changes in the ``mybranch`` branch.

Once you have merged your changes, you can delete the branch by calling

::

	geogit branch -d mybranch


Solving merge conflicts
-------------------------

When merging two branches, the changes made in them might not be compatible. For instance if both branches have modified the same attribute value of the same feature, setting a different new value, that will cause a conflicting situation that GeoGit cannot solve automatically, since it cannot decide which one of the changes it should keep. In this case, it will merge all the compatible changes and leave the conflicting ones marked, so they can be manually resolved and then committed.

When a merge operation finds conflicts and it cannot automatically merge all the changes, it shows a message like the one shown below:

::

	CONFLICT: Merge conflict in parks/parks.2
	CONFLICT: Merge conflict in parks/parks.3
	CONFLICT: Merge conflict in parks/parks.1
	Automatic merge failed. Fix conflicts and then commit the result.

The following is a list of situations that will cause a merge conflict:

- Both branches have modified the same attribute of a feature, setting different values.
- One branch has modified an attribute in a feature, while the other has deleted that attribute.
- Both branches have added different features under the same path.
- One branch has deleted a feature while the other has modified it.
- Both branches have modified the default feature type for a given path, setting different values.
- Both branches have modified the geometry of a feature, and there is no way of applying both changes, for instance, if both have modified the same point in a polygon.
- One branch has deleted a tree, while the other one has added or modified a feature under that tree.


The following cases will not produce a merge conflict:

- Both branches have a added the same feature at the same path.
- Both branches have deleted the same feature.
- Both branches have modified the same attribute in a feature, setting the same new value.
- Both branches have set the same new default feature type for a path.
- Both branches have edited the same feature, but modifying different attributes.
- Both branches have modified the geometry of a feature, but changes are compatible and can be both incorporated, for instance if each one has moved a different point in a polygon, leaving the remaining points unchanged.
- Both branches have made the same modification to a feature geometry.
	
When a conflict arises, the merge operation is interrupted. Conflicted elements are marked in the index and the user should solve them manually before committing to complete the merge operation.

You can check which elements are conflicted by running the ``geogit status`` command. The result will be similar to the one shown next:

::

	# On branch master
	# Unmerged paths:
	#   (use "geogit add/rm <path/to/fid>..." as appropriate to mark resolution
	#
	#      unmerged  parks/parks.2
	#      unmerged  parks/parks.3
	#      unmerged  parks/parks.1
	# 3 total.

The above message shows a repository with just 3 conflicted features in its index. If there are unstaged elements, they will also be shown, as usual. Also, if the merge operation staged elements that did not cause any conflict, they will appear as ready to be committed. These elements will also be changed in the working tree, to reflect the same version that is stored in the index, which is the result of the automatic merge operation.

In order to fix the conflicts in the staging area, several GeoGit tools and approaches can be used. These are described below.

Staging a merged version of an unmerged (conflicted) element. 
-------------------------------------------------------------

Using the ``geogit add`` command, features can be staged in the usual way. When a feature is staged, it is no more in a conflicted state. After a conflicted merge, the working tree version of a conflicted element remains unchanged (notice that this is different to git, which edits the working tree an sets a version with conflict markers. The rest of the process is, however, similar). If you run the ``add`` command, you will be solving the conflicted merge by setting the version in the current branch (the 'ours' version) as the good one, and rejecting changes for that feature coming from the other branch that is being merged onto the current one (that is, rejecting the 'theirs' version).

If you want to stage a different version, you can use one of the following procedures to set a different feature in the working tree before running the ``add`` command.

- Import a new feature using one of the several importing tools from GeoGit
- Set the version from the branch to merge (the 'theirs' version) by running ``geogit checkout --theirs``
- Delete the feature using the ``rm`` command. This will remove it from both the working tree and the index, and will remove the conflict mark from the index as well. Their is no need to call ``add`` afterwards, unless you have staged some other element to solve a different conflict, using any of the other methods described above.

Once you have the correct version that you want to commit, run ``add`` to stage it and then run ``commit`` to finally commit your resolved elements and finish the merge.

When you run the ``commit`` command, you usually must supply a commit message using the ``-m`` switch. You can do it like that in this case, but you can also run it without a commit message. Since the commit is part of a merge operation that was interrupted due to conflicts, GeoGit will have prepared a default commit message. In the conflict case shown above, the default message would look like this:

::

	Merge branch refs/heads/b1

	Conflicts:
		parks/parks.2
		parks/parks.3
		parks/parks.1


Aborting the merge operation
-----------------------------

You can abort the merge operation and restore it to the original state it had before you invoked the ``merge`` command. You have the following alternatives, which will cause the same result [NOTE: this is not like git, the --abort here is just a reset op, but not in git]

- ``geogit reset --hard ORIG_HEAD``
- ``geogit merge --abort``


Solving using the merge tool
------------------------------

The most practical way to solve the merge conflicts is using the merge tool.

[To Be Written]


The ``mergetool`` command can also be used to describe the current unmerged elements. There are two ways of displaying conflicts: the first uses the ``--preview`` option and it prints the full description of the three versions involved in the conflict (the common ancestor, 'ours' and 'theirs'). It looks like the example shown next, corresponding to a single unmerged feature.

::
	
	parks/parks.2

	Ancestor    27207309879802a99d161b063b8f958d179be3b0
	FEATURE
	id    27207309879802a99d161b063b8f958d179be3b0
	java.lang.String    Medford School District
	java.lang.Double    53935.8939996
	java.lang.Double    1004.9211325
	java.lang.String    Kennedy Elementary
	java.lang.Long    0
	java.lang.String    Medford School District
	java.lang.String    School Field
	com.vividsolutions.jts.geom.MultiPolygon    MULTIPOLYGON (((-122.84163143974176 42.35985624789982, -122.84146965654989 42.35985609227347, -122.84117673733482 42.35985565827537, -122.8409230724077 42.35985528171881, -122.84062434545373 42.35985483812396, -122.84034728245699 42.35985442523742, -122.8403468719201 42.35943411552068, -122.84163015984652 42.35942328456196, -122.8416300075414 42.359625066567794, -122.84163143974176 42.35985624789982)))
	java.lang.String    Public


	Ours    d8cc931603603bd64506880dc1760b372808ef2d
	FEATURE
	id    d8cc931603603bd64506880dc1760b372808ef2d
	java.lang.String    Medford School District
	java.lang.Double    53935.8939996
	java.lang.Double    1004.9211325
	java.lang.String    Kennedy Elementary
	java.lang.Long    5
	java.lang.String    Medford School District
	java.lang.String    School Field
	com.vividsolutions.jts.geom.MultiPolygon    MULTIPOLYGON (((-122.84163143974176 42.35985624789982, -122.84146965654989 42.35985609227347, -122.84117673733482 42.35985565827537, -122.8409230724077 42.35985528171881, -122.84062434545373 42.35985483812396, -122.84034728245699 42.35985442523742, -122.8403468719201 42.35943411552068, -122.84163015984652 42.35942328456196, -122.8416300075414 42.359625066567794, -122.84163143974176 42.35985624789982)))
	java.lang.String    Public


	Theirs    a77e46d2ad6e2c9eef3b6e5191a6c299037d602c
	FEATURE
	id    a77e46d2ad6e2c9eef3b6e5191a6c299037d602c
	java.lang.String    Medford School District
	java.lang.Double    53935.8939996
	java.lang.Double    1004.9211325
	java.lang.String    Kennedy Elementary
	java.lang.Long    2
	java.lang.String    Medford School District
	java.lang.String    School Field
	com.vividsolutions.jts.geom.MultiPolygon    MULTIPOLYGON (((-122.8434107328942 42.36043884831257, -122.84324894970233 42.360438692686216, -122.84295603048726 42.36043825868812, -122.84270236556014 42.360437882131556, -122.84240363860617 42.36043743853671, -122.84212657560943 42.36043702565017, -122.84212616507254 42.360016715933426, -122.84340945299896 42.36000588497471, -122.84340930069384 42.36020766698054, -122.8434107328942 42.36043884831257)))
	java.lang.String    Public

The descriptions of the involved elements are the same ones that would be obtained by calling the GeoGit ``cat`` command on each of them.

A representation with diff-like syntax instead of full descriptions can be obtained using the ``--preview-diff`` option. For the same unmerged feature described above, the resulting output would look like this:

::

	---parks/parks.2---
	Ours
	number_fac: 0 -> 5

	Theirs
	number_fac: 0 -> 2
	the_geom: MultiPolygon [-122.84163143974176,42.35985624789982 -122.84146965654989,42.35985609227347 -122.84117673733482,42.35985565827537 -122.8409230724077,42.35985528171881 -122.84062434545373,42.35985483812396 -122.84034728245699,42.35985442523742 -122.8403468719201,42.35943411552068 -122.84163015984652,42.35942328456196 -122.8416300075414,42.359625066567794 -122.84163143974176,42.35985624789982] (-122.8434107328942,42.36043884831257 -122.84324894970233,42.360438692686216 -122.84295603048726,42.36043825868812 -122.84270236556014,42.360437882131556 -122.84240363860617,42.36043743853671 -122.84212657560943,42.36043702565017 -122.84212616507254,42.360016715933426 -122.84340945299896,42.36000588497471 -122.84340930069384,42.36020766698054 -122.8434107328942,42.36043884831257)

It uses the same syntax as the ``diff`` command, which is described in the `Showing differences`_  section. This makes it easier to see why the conflict arises and how to solve it.