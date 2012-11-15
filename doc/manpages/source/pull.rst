
.. _geogit-pull:

geogit-pull documentation
#########################



SYNOPSIS
********
geogit pull [options] [<repository> [<refspec>...]]


DESCRIPTION
***********

Incorporates changes from a remote repository into the current branch.

More precisely, geogit pull runs geogit fetch with the given parameters and calls geogit merge to merge the retrieved branch heads into the current branch. With --rebase, it runs geogit rebase instead of geogit merge.

OPTIONS
*******

--all       Fetch all remotes.

--rebase    Rebase the current branch on top of the upstream branch after fetching.

SEE ALSO
********

:ref:`geogit-fetch`
:ref:`geogit-clone`
:ref:`geogit-rebase`

BUGS
****

Discussion is still open.

