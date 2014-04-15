
.. _geogit-config:

geogit-config documentation
###########################



SYNOPSIS
********
geogit config [--global|--local] name [value]
geogit config [--global|--local] --get name
geogit config [--global|--local] --unset name
geogit config [--global|--local] --remove-section name
geogit config [--global|--local] -l
 


DESCRIPTION
***********

You can query/set/unset options with this command. The name is actually the section and the key separated by a dot, and the value will be escaped.

By default, the config file of the current repository will be assumed.  If the --global option is set, the global ``.geogitconfig`` file will be used. If the ``--local`` option is set the config file of the current repository will be used if it exists.

OPTIONS
*******

--global            Tells the config command to use the global config file, rather than the repository config file.

--local				Tells the config command to use the repository config file, rather than the global config file.

--get               Query the config file for the given section.key name.

--unset             Remove the line matching the given section.key name.

--remove-section    Remove the given section from the config file.

-l, --list          List all variables from the config file.

GEOGIT CONFIGURATION
********************

This list is not comprehensive; some configuration options are documented in relevant man pages.

bdbje.object_durability     Determines how safe to be when persisting objects in the BDB object store.  Valid values include: safe (be as safe as possible) and fast (sacrifice some safety to improve performance.)

SEE ALSO
********

geogit-init

BUGS
****

Discussion is still open.

