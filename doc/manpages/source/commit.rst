
.. _geogit-commit:

geogit-commit documentation
############################



SYNOPSIS
********
geogit commit [-m <msg>] [<path_filter>...] [-c <commitish>] [--amend]


DESCRIPTION
***********

Stores the current contents of the index in a new commit along with a log message from the user describing the changes. If path filters are passed, only those features passing the filter will be commited.

A message must be provided, unless the ``-c`` switch or the ``--amend`` switch are used. Also, if commiting from a merge conflict state, no message is needed, since the merge message will be reused.


OPTIONS
*******

-m <msg>    		Use the given <msg> as the commit message.

-c <commitish> 		Reuses values (message, author, author timestamp) from a previous commmit, instead of creating them. If a message is provided, the message of the specified commit will be ignored

--amend 			Amend the last commit. It reuses the information from the previous commit, but applied to the current changes. If a message is provided, the message of the commit to amend will be ignored



SEE ALSO
********

:ref:`geogit-add`

:ref:`geogit-status`

BUGS
****

Discussion is still open.

