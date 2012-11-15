
.. _geogit-clone:

geogit-clone documentation
#########################



SYNOPSIS
********
geogit clone [--branch <name>] <repository> [<directory>]


DESCRIPTION
***********

Clones a repository into a newly created directory, creates remote-tracking branches for each branch in the cloned repository (visible using geogit branch -r), and creates and checks out an initial branch that is forked from the cloned repository's currently active branch.

After the clone, a plain geogit fetch without arguments will update all the remote-tracking branches, and a geogit pull without arguments will in addition merge the remote master branch into the current master branch, if any.

This default configuration is achieved by creating references to the remote branch heads under refs/remotes/origin and by initializing remote.origin.url and remote.origin.fetch configuration variables.

OPTIONS
*******

-b <name>, --branch <name>    Branch to checkout when clone is finished.

SEE ALSO
********

:ref:`geogit-fetch`
:ref:`geogit-pull`
:ref:`geogit-push`

BUGS
****

Discussion is still open.

