Creating branches
=================

Up to this point, we have worked in a linear fashion, adding new commits one after another. In some circumstances, it might be interesting to have several histories in our repository, creating a tree-like structure. GeoGit allows that, by creating so-called branches.

The following figure shows the difference between the linear workflow we have been describing until now and a branch-based one.

.. figure:: branches.png

Branches are useful if you want to work on your data and version your changes, but you do not want to mix your new work with the rest of the data until a certain point, usually when it reaches a given completion state. For instance, imagine that you have a rather large dataset and you want to edit it by adding new attributes to its features. That might take some time of work, and if you keep using a linear model, during that time the updated features will coexist with the original unedited ones, which might cause problems for users of the repository. You can do all your work and not commit anything until you have finished, but that is less convenient, since you cannot publish your data and keep versions of it as you work. Instead, creating a branch gives you a new context where you can work, not interfering with the main branch. Once you have finished your work and it is ready to replace the old feature from where you started your work, you can pass your changes to the main branch, as we will see in an upcoming section.

Each branch has a name that is given to it when it is created. The "central" branch that is created when you initialize the repository, in which we have been working until now, it is called ``master``.

To create a new branch named ``edits`` use the following command:

::

	$geogit branch mybranch
	Created branch refs/heads/edits

This creates a new branch, but does not put you into it. To change to that branch you have to perform a *checkout*. You can do it using the ``checkout`` command followed by the name of the branch, or by adding the ``-c`` option when creating the branch.

Let's move to our ``edits`` branch

::

	$geogit checkout edits

When you checkout a branch, GeoGit puts the data from the tip of that branch (its latest commit) in you working tree. For this reason, your working tree has to be clean, with no unstaged changes, before doing a checkout. 

When a branch is created, a reference is created as well with the name of the branch. That reference will always point to the latest commit made on that branch. When you checkout a branch, GeoGit changes the ``HEAD`` to point at the reference denoting the branch you have checked out. Until now, ``HEAD`` was linked to the ``master`` branch and its corresponding reference, but when you checkout a branch, ``HEAD`` changes automatically.

You can start working and creating new commits, and they will be added on top of the current branch, instead of on top of ``master``, as shown in the next figure:

..figure:: work_on_branch.png

You can go back to the ``master`` branch, just by checking it out. If you have done no work on ``master`` (by moving to it and making commits) since then, that will take you to the state that it had when you created the ``edits`` branch, and the working tree content will correspond to it. That is, your work on the ``edits`` branch is completely independent of the current contents of ``master``.

You can consider that having branches is somehow like having several repositories in which you work in a linear fashion, but since everything is kept in the same repository, you can easily move between them.

The name of the branch is a valid reference just like the ones you already know (``WORK_HEAD, STAGE_HEAD``, etc.). For this reason, you can use it to reference an object under that branch. For instance, to show a description of a feature named ``parks.1`` in a tree called ``parks``, in its version in the ``edits`` branch, you can use the following command:

::

	$geogit show edits:parks/parks.1

As we mentioned, the first part of the reference (the part before the colon), has to eventually resolve to a tree. The ``edits`` reference points to a commit (the latest one on that branch), and a commit points to a tree as we already know, which makes ``edits`` a valid string to use for creating a full reference.



