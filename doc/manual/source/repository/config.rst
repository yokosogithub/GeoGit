.. _configuring:

Configuring a GeoGit repository
================================

Your repository is already initialized, but it does not contain any data yet. Before actually starting working on your repository and using it for versioning your data, a bit of configuration is needed to ease further work.

GeoGit configuration parameters are set using the ``geogit config`` command, which, in the case of setting a new value, has the following syntax:

::

	geogit config <--global> param_name param_value

Parameters are divided in sections, and they are referred using a syntax in the form ``section.parameter``.

We are going to set two parameters in the ``user`` section, which will identify you as the user of the repository. When you later add changes to the repository to create new versions of your data, GeoGit will know who you are and use that information to label the corresponding commit, so you do not have to enter it manually each time. User information is specially important when you share your changes with other people, so everyone can know who has made each change.

To define your user, you have to configure a name and an email address, using the ``user.name`` and ``user.email`` parameter. Here is an example that shows how to do it. Substitute the email and name values with your own ones.

::

	$ geogit config user.name "volaya"
	$ geogit config user.email "volaya@opengeo.org"

You can get a listing of all configured values by using the ``-l`` option. 

::

	$ geogit config -l
	user.name=volaya
	user.email=volaya@opengeo.org

The user identity has been correctly configured

Now, whenever GeoGit needs to add user information to an operation, it will use these values.

The variables we have set are valid just for the repository where you have configured them. If you have many repositories and you do not want to configure the same parameters for each one, you can configure *global* parameters as well. A global parameter value is used by GeoGit if the current repository does not have its own particular values for that parameter. This way, you can init a repository, and start working on it without any further configuration, since GeoGit will use your global settings.

To set the global value of a parameter, use the ``--global`` option.

::

	$ geogit config --global user.name "volaya"
	$ geogit config --gloabl user.email "volaya@opengeo.org"

You can use it as well for listing all currently set global variables.

::

	$ geogit config -l --global
	user.name=volaya
	user.email=volaya@opengeo.org
