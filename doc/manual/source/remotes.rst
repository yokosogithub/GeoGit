Interacting with remote repositories
=====================================

A GeoGit repository contains a full history and it is completely autonomous. You can use it to store and version your data, and there is no need to use any other repository. However, interacting with other repositories that keep the same data is interesting, since it allows different people to share and put in common the changes that each of them is introducing in its own repository. By having a designated central repository which keeps a reference history, others can clone it, work on a cloned repo and then add their changes back to the central repository, allowing to distribute the work while keeping data correctly organized. Unlike other systems where only the central repository keeps the full history, in GeoGit all repositories have complete history, and the central one is actually no different from the other cloned copies.


Remotes. Cloning a repository
===============================

To clone a repository, the ``clone`` command is used. You must specify the name of the folder where the cloned repository is to be stored. And of course, you must supply a valid URL that points to the original repository you want to clone.

Here is the command line to be used to clone a repository at ``http://myoriginalrepo.com`` into a folder named ``repo``

::

	$geogit clone http://myoriginalrepo.com repo


Once the repository is cloned, both copies are identical, and you can start working on your copy independently, following the usual GeoGit workflow. There is no need to use the original repository, but that doesn't mean that it cannot be done. In fact, it is interesting to keep repositories connected, since that way they can share their changes. From your repository, you can connect to any number of repositories that version the same data, get their changes and also include you changes in them if you have write access. That bidirectional communication between repositories makes it easy to have a flexible and effective collaboration model.

For a given repository, all other repositories that it is connected to are known as *remotes*. Instead of referring to a remote with its full URL, a GeoGit repository can keep track of them using alias, and these alias can be used when a reference to a remote is needed as an argument for a GeoGit command.

Remotes are added using the ``geogit remote`` command, as in the following example.

::

	$geogit remote add origin https://myoriginalrepo.com


Pushing and pulling
---------------------

With the remote repositories already configured, you can now interact with them from your local repository. Basically, there are two things that you can do: *push* changes (apply them on a remote repository) or *pull* them (apply on your repository changes from another repository). The ``push`` and ``pull`` commands are used for this purpose.

The following figure, that we already saw in the introduction, summarizes the above mechanism.

.. figure:: ../img/geogit_workflow_remotes.png


To push changes to a remote repository named ``origin``, the following line is used.

::

	$geogit push origin

Of course, you must have write access to that repository. The command will push the current branch you are in. 

To be able to push a branch, you must have in your repository the latest changes made in the remote repository in that branch, so your changes can be added on top of them by just adding the commit corresponding to your own work.

Retrieving the changes in a remote repository is done using the pull command, as shown below.

::

	$geogit pull origin master

That would bring all changes from the ``master`` branch in the ``origin`` repository into the current branch of the local repository. You can be in a branch other than ``master``. There is no need to specify the same branch as the current branch in the local repository. GeoGit will grab the commits that are missing in your local branch after comparing with the remote branch, and will merge them. Of course, this merge is not guaranteed to be clean, and conflicts might appear. They are solved in much the same way as a local merge conflict.

If instead of a merge you want to perform a rebase, then you can use the ``--rebase`` option. It will rewind your ``HEAD`` to the point were it was before you synchronized it the last time with the remote repository, then apply all the new changes that might exist in the repository, and then re-apply all you local changes on top of the updated ``HEAD``. As in the case of a local rebase, conflict might also arise when pulling with the ``--rebase`` option.

With this two commands, you can now interact with other repositories and set a collaboration model among them.


Using ``fetch``
---------------

The ``pull`` operation is actually a compound of two operations: ``fetch`` and ``merge``. The ``fetch`` operation brings all the changes from the corresponding remote branch, setting a branch in your repo that contains them. Once this is done and the data is stored locally, the merge is performed.

Using the ``fetch`` command independently allows you to track branches in a remote repository, and also to *bring* new branches into your repository if they have been created in the remote repository. When the corresponding local branches that contain the changes and data of their remote counterpart are created, you can use them as you would do with any other local branch. This gives you a more fine-grained functionality, which allows you to have more flexibility than jusst using the ``pull`` command.

[To Be written]