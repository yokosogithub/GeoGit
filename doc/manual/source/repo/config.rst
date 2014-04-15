.. _repo.config:

Configuring a GeoGit repository
===============================

Once the repository is initialized, some further configuration is recommended to ease further work.

GeoGit configuration parameters are set using the ``geogit config`` command, which, in the case of setting a new value, has the following syntax:

.. code-block:: console

   geogit config <--global> param_name param_value

Parameters are divided in sections, and they are referred to in the form ``section.parameter``.

When creating a new repository, we recommend that you set two parameters in the ``user`` section, which will identify you as the user of the repository. This is especially useful when sharing changes with others. When you later add changes to the repository to create new versions of your data, GeoGit will know who you are, so you will not have to enter this information manually each time. 

To define your user, you have to configure a name and an email address, using the ``user.name`` and ``user.email`` parameter.  Substitute the values in quotes with your own name and email address.

.. code-block:: console

   geogit config user.name "Author"
   geogit config user.email "author@example.com"

You can get a display of all the configured values by using the ``-l`` option. 

.. code-block:: console

   geogit config -l

.. code-block:: console

   user.name=Author
   user.email=author@example.com

These parameters are valid just for this particular repository. You can also configure global parameters as well, which will apply to all repositories created, unless overwritten by a local parameter.

To set the global value of a parameter, use the ``--global`` option.

.. code-block:: console

   geogit config --global user.name "Author"
   geogit config --global user.email "author@example.com"

You can also get a display of all the configured global values by using the ``-l`` option. 

.. code-block:: console

   geogit config -l --global

.. code-block:: console

   user.name=Author
   user.email=author@example.com
