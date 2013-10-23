.. _advconfig:

Advanced configuration
======================

This section describes some advanced ideas to configure GeoGit.

Aliases
-------

GeoGit supports aliases. You can define an alias that can be later invoked to call a command with a shorter name. For instance, you can define replace the command "commit --amend" by "am", by running the following command

::

	$ geogit config alias.am commit --amend

To amend a command, now you just need to call

::

	$ geogit am

To add an alias, a new configuration variable has to be added with the name ``alias.<name_of_alias>``, and the value that GeoGit should replace the alias with when it is invoked.

If you want the alias to be available for the current repository, just call the config command with no extra options, as in the example above. If you want it to be available system-wide, use the ``--global`` switch to create a global configuratino variable.

Additional arguments can be added when using aliases, as they are done when using the full command.