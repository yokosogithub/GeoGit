Introduction
============

GeoGit is a Distributed Version Control System (DVCS) specially designed to handle geospatial data efficiently. It takes inspiration from the source code versioning system *git*, but has a different approach, best suited to the different nature of the data it manages. Users familiar with *git* should find it easy to use GeoGit, but reading this documentation is recommended, since some commands and ideas differ from the corresponding *git* ones. For users new to versioning systems, this document provides a complete description, and it does not assume the reader is familiar with any of such systems. Although this type of software, and particularly *git*, is originally used by programmers to manage their source code, this document is not targeted at programmers, and does not assume the reader has programming experience.

For those readers familiar with *git*, there are notes along the text that compare *git* and GeoGit and that will help you better understand the differences between both softwares. They look something like this:

.. note:: This is a note for *git* users

Users not familiar with *git* should skip notes like the one above.

This user guide does not cover all possible uses of each GeoGit command, but just introduces the most common cases and the operations used by the great majority of users. Once you understand the concepts explained here, feel free to explore the GeoGit man pages to learn more about other options not covered here.

All GeoGit commands accept a ``help`` option that will display the available options and their usage. Use it when you have questions about how to use the command, or to find out which other possibilities exist apart from the one described here.

.. note:: Geogit is still being developed, and some functionality might change or not be fully usable yet. This documentation reflects most of the planned functionality that should be available once a stable version is released. While almost everything described here is already implemented and functioning, you might find some commands or options that are not completely in the state described in this documentation, or that might have a different behaviour. Eventually, those commands should work as described here, but at certain points, the documentation might be ahead of the actual implementation.


How does GeoGit work
---------------------	

The following is a brief introduction to how GeoGit works and how it handles your data, and also how a GeoGit repository interacts with other repositories. These concepts will be explained in detail in the following sections, as we explain the corresponding commands that are used to set up a GeoGit repository and work on it.

GeoGit stores its content in a repository which has three areas: the working tree, the staging area, and the database.

- The working area is the area of the repository where the work is actually done on the data. Data in the working tree is not part of a defined version, but instead is data that you can edit and alter before turning it into a new version that will be safely stored, and that you can retrieve back at anytime. That means that if you put data on the working tree and change it, the previous version cannot be recovered, unless you have created a new version from it by copying it to the repository database.
- The staging area, sometimes also referred as index, is an intermediate area where data is stored before moving it to to repository database. Data in the staging are is said to be staged for committing, a previous step needed to commit the data and finally creating a new version.
- The repository database is where the history of the repository is stored, and it keeps all the versions that have been defined 

The process of versioning your geospatial data is basically a process of moving data from the working tree to the staging area and the repository database (in the case of adding new changes and creating a new version), or moving it back from the database to the working tree (in case of recovering a previous version to work with it). Previous to that, you must import your geospatial data into the working tree, so it can be managed by GeoGit.

The following figure summarizes the above concepts.


.. figure:: ../img/geogit_workflow.png

The names in the arrows indicate the corresponding commands for each operation. All those commands will be described in the following sections.


As you add new data to the repository database, GeoGit creates new versions that define the history of the repository. While some versioning systems store the differences between consecutive versions, GeoGit stores the full set of objects that comprise each version. For instance, if you have changed a feature by modifying its geometry, GeoGit will store the full definition of that feature, and it will be kept in the repository database along with the previous version of the same feature. For features not modified from one version to another, the corresponding objects are not stored again, but the new version points to the same previous object. In other words, each version is a new set of objects, but the data for these objects is not stored redundantly, but just once for each one of them.

.. todo:: The following figure explains this idea. 


GeoGit is designed to ease collaboration among people working on the same dataset. Your repository it is not isolated (unless you want to), and the history it contains doesn't have to be necessarily the history of only the changes that you have introduced yourself locally. It can take changes from other people working on the same data, and you can share with them your own changes. Instead of a single repository, there will be an ecosystem of connected repositories, each of them working independently, but communicating when needed. GeoGit has tools to make this collaboration as easy as possible, and to ensure a fluid coordination between a group of collaborators using and editing the same dataset.

The following image shows an extended version of the GeoGit workflow presented before, including the interaction with other GeoGit repositories.

.. figure:: ../img/geogit_workflow_remotes.png

GeoGit is independent of the storage format that you use for your data and the applications you run to edit or analyze it. It will just take care of versioning it, ensuring that for each new version that you create, it is stored safely and you can recover it when you need it. That means that your repository might share its data with the repository of another collaborator, and you can both edit that data and work on it, but each of you can use a different software to do it. Once your work is done and your data ready to be included in a new version, GeoGit will do its work, since it integrates orthogonally with applications and data formats.