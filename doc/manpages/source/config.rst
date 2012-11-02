
.. _geogit-config:

geogit-config documentation
#########################



SYNOPSIS
********
geogit config [--global] name [value]
geogit config [--global] --get name
geogit config [--global] --unset name
geogit config [--global] --remove-section name
geogit config [--global] --l
 


DESCRIPTION
***********

You can query/set/unset options with this command. The name is actually the section and the key separated by a dot, and the value will be escaped.

By default, the config file of the curent repository will be assumed.  If the --global option is set, the global .geogitconfig file will be used.

OPTIONS
*******

--global            Tells the config command to use the global config file, rather than the repository config file.

--get               Query the config file for the given section.key name.

--unset             Remove the line matching the given section.key name.

--remove-section    Remove the given section from the config file.

-l, --list          List all variables from the config file.

BUGS
****

Discussion is still open.

