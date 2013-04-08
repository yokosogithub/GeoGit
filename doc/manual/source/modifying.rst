Modifying history
==================

Once you make a commit, a new version of the data in your repository is created and stored, along with the previous one. The full set of commits and objects in the repository database constitutes its history. Ideally, you should change the history of a repository just by adding new commits. However, sometimes it is useful to rewrite parts of the history or to change its structure, to more clearly reflect the work that it has been done. GeoGit provides several commands and options than alter the current history of a repository. We will discuss them in this chapter.


Amending a commit
------------------

A very typical case of history modification happens when you have forgot to add something to the last commit. For instance, you had staged a feature, but forgot to add a second one that you created later. When you made your commit, only the staged feature will be committed, while the second one will exist only in the working tree.

You can create a second commit with it, but it would be much better to have both features in the same commit, since that's how it was supposed to be, and that way you history will be clearer and more compact. to avoid creating a new commit, you can amend the last commit and tell GeoGit to combine it with the new commit that you are now going to make (the one with the second feature). To amend a commit, use the ``--amend`` option when making the commit.

::

	$geogit commit --amend

As you can see, there is no need to add a commit message, since the commit command will use the one from the previous commit. It will replace the previous commit with a new one that include the extra changes that you might have introduced, but keep the same message.

If you want to change the message, you can add one the usual way. In that case, the previous message will be ignored, and a full new commit created, which will replace the last one.


Squashing commits
-----------------

A larger modification of the history can be made by replacing a given set of consecutive commits with a single one. This is usually the case when you have been working on data corresponding to a given task (for instance, adding features within a given area), but you have and made several commits before fully completing it. To tidy up your repository and have a more compact history, you would like to have all those commits into a single one.

You can squash commits by using the ``squash`` command and providing it with a range of commits. All those commits will be squashed into just one. For instance, to squash the last 3 commits, you can use the following sentence.

::

	$geogit squash HEAD~2 HEAD

The message is taken from the oldest commit of all the ones being squashed, in this case HEAD~2.

The set of commits to squash, doesn't have to be at the tip of the current branch. You can squash commits in the middle of the history line as well.

::

	$geogit squash HEAD~5 HEAD~2

A message is not needed, but it can be supplied if needed, using the ``-m`` option.

::

	$geogit squash HEAD~5 HEAD~2 -m "Cleaned up small polygons"


When performing a rebase operation, the commits that are rebased can be squashed into one automatically, by adding the ``--squash`` option, as we saw in the corresponding chapter.

.. note::

	Squashing is performed in ``git`` by doing an interactive rebase. The rebase operation in GeoGit doesn't have an interactive mode, so an additional command has been added for this purpose.


Splitting a commit
-------------------

The opposite case to what we have just seen is also common: splitting an already made commit in several ones. There is no command for doing that in GeoGit, but you can get that same result by running a soft reset.

::

	$geogit reset --soft HEAD~1

That will put your HEAD one commit behind, discarding that last commit. However, your working tree and index will not be affected, so they will be left unchanged. You can now commit the changes they have, but doing it in as many commits as you need, which will replace the larger commit that you have just discarded.

This can only be applied if the commit to split is the last one (or a set of them in the tip of the branch).


A note on modifying the history of a repo
------------------------------------------

Modifying the history of a repository is safe as long as you are not sharing it and you are sure that no one else is basing his work on it. If that is not the case, pull operations from other repositories whose history has not been rewritten in the same way are likely to involve conflicts and trouble

.. todo:: rewrite when --force option is added to pull
