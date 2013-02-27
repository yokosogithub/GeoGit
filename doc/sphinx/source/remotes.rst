Interacting with remote repositories
=====================================

A GeoGit repository contains a full history and it is completely autonomous. You can use it to store and version your data, and there is no need to use any other repository. However, interacting with other repositories that keep the same data is interesting, since it allows different people to share and put in common the changes that each of them is introducing in its own repository. By having a designated central repository which keeps a reference history, others can clone it, work on a cloned repo and then add their changes back to the central repository, allowing to distribute the work while keeping data correctly organizes. Unlike other systems where only the central repository keeps the full history, in GeoGit all repositories have complete history, and the central one is actually no different from the other cloned copies.

To clone a repository, the ``clone`` command is used. You must specify the name of the folder where the cloned repository is to be stored. And of course, you must supply a valid URL that points to the original repository you want to clone.

************

Once the repository is cloned, both copies are identical, and you can start working on your copy independently, following the usual GeoGit workflow. There is no need to use the original repository, but that doesn't mean that it cannot be done. In fact, it is interesting to keep repositories connected, since that way they can share their changes. From your repository, you can connect to any number of repositories that version the same data, get their changes and also include you changes in them if you have write access. That bidirectional communication between repositories makes it easy to have a flexible and effective collaboration model.

For a given a repository, all other repositories that it is connected to are known as *remotes*. Instead of referring to a remote with its full URL, a GeoGit repository can keep track of them using alias, and these alias can be used when a reference to a remote is need as an argument for a GeoGit command.

Remotes are added using the ``geogit remote`` command, as in the following example.

::

	$geogit remote add origin https://myoriginalrepo.com


With the remote repositories already configured, you can now interact with them from your local repository. Basically, there are two things that you can do: *push* changes (apply them on a remote repository) or *fetch* them (apply on your repository changes from another repository). The ``push`` and `` fetch`` commands are used for this purpose.
