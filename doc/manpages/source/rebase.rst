
.. _geogit-rebase:

geogit-rebase documentation
#########################



SYNOPSIS
********
geogit rebase [--onto <newbase>] [<upstream>] [<branch>]


DESCRIPTION
***********
Forward-port local commits to the updated upstream head.

If <branch> is specified, geogit rebase will perform an automatic geogit checkout <branch> before doing anything else. Otherwise it remains on the current branch.

All changes made by commits in the current branch but that are not in <upstream> are saved to a temporary area.

The current branch is reset to <upstream>, or <newbase> if the --onto option was supplied.

The commits that were previously saved into the temporary area are then reapplied to the current branch, one by one, in order.

OPTIONS
*******    

--onto <newbase>    Starting point at which to create the new commits. If the --onto option is not specified, the starting point is <upstream>. May be any valid commit, and not just an existing branch name.

SEE ALSO
********

:ref:`geogit-log`

BUGS
****

Discussion is still open.

