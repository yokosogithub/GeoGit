Checking out a previous version
=================================

All the versions stored in a GeoGit repository are available an can be used. We already know how to refer to an object from a previous version, by using the reference syntax explained in `Referencing a GeoGit element`_. That allows us to describe that element or to use it for a certain operation.

A different way of recovering a given version of the data kept in the repository is to bring all the changes to the working tree, so we can actually work on that data and. Notice that this could be used, for instance, to export it and make that version of the repository available for an external application. However, you can export from a given commit without having to checkout and then export from the working tree, as it was explained in the `Exporting from a GeoGit repository`_ section.

The ``add`` and ``commit`` commands *move* the data from the working tree into the staging area, and from there into the repository database. That same data can go the opposite way, from the repository database to the working tree. In that direction, the index is skipped and the working tree is updated directly from the repository database.

To checkout a past version of the repository data, the ``checkout`` command is used, just in the same way as we use it to move from one branch to another. Instead of a branch name, you must supply the name of a commit (its ID), and data corresponding to that commit will be put in the working tree. Since the data in the working tree will be overwritten, this command cannot be run when the working tree has unstaged changes.

The following is a valid workflow that shows how to update the version in the working tree from the current one to the snapshot corresponding to 5 commits ago.

::

	$geogit checkout HEAD~5
	TODO******


Apart from updating the working tree, the ``checkout`` command updates the HEAD ``reference``, which will now point to the commit from where the data to update the working tree was taken. 

You can now export the current working tree to a shapefile, and external applications will be able to use the old version of the data, which you have exported and is now in that shapefile.

To go back to the most recent state, where you were before checking out the previous version,  you have to checkout the latest commit on the current branch. Notice that ``HEAD`` is not pointing now to that commit, so you will have to use the name of the current branch. Assuming you are in the ``master`` branch, the following will update the working tree to the latest version, and change the ``HEAD`` reference to the corresponding commit.

::

	$geogit checkout master

Reseting to a previous commit
------------------------------

When you perform a checkout using a commit, the ``HEAD`` reference points directly to the commit. Usually, ``HEAD`` is itself a symbolic reference, and it point to the tip of a given branch. If we are in ``master``, then ``HEAD`` points to whereever ``master`` is pointing. If there is a commit, the tip of the branch changes, and ``HEAD changes automatically.

When ``HEAD`` is pointing to a commit directly, it is said to be in a *detached* state. You should not make commits in that state, because they will not be added to the tip of you current branch.

If what you want to do is to revert to a previous snapshot of the current branch and start working from there, then you should use the command ``reset`` instead of checkout. The ``reset`` command will move the tip of the current branch (the ``master`` reference in this case) back to an specified commit, and ``HEAD`` will move along. Now you can start your work, which will be added on top of the commit to which to have reset the branch.

To reset to the commit 5 commits ago, use the following:

::

	$geogit reset HEAD~5 --hard

That will update all 3 areas in GeoGit (working tree, staging area and database) to the specified commit. This is known as a hard commit. You can also perform a mixed reset (only updates the staging area and database, but not the working tree, with the ``--mixed`` option), or a soft reset (only updates the database, with the ``--soft`` option).


