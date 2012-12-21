
.. _geogit-merge:

geogit-merge documentation
##########################



SYNOPSIS
********
geogit merge [-m <message>] <commitish>...

DESCRIPTION
***********
Incorporates changes from the named commits (since the time their histories diverged from the current branch) into the current branch. This command is used by ``geogit pull`` to incorporate changes from another repository and can be used by hand to merge changes from one branch into another.

OPTIONS
*******    

--m <message>    Commit message.  If a message is not provided, one will be created automatically.

SEE ALSO
********

:ref:`geogit-log`
:ref:`geogit-pull`
:ref:`geogit-commit`

BUGS
****

Discussion is still open.

