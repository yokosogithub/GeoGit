.. _installing:

Installation
=============

GeoGit has to be installed in your system before you can start using it for versioning your spatial data. This sectin describes the steps needed to install GeoGit.

No packaged versions are currently available for GeoGit, and it has to be build from sources. To build GeoGit and have an executable binary that you can run, follow these steps.

- Clone the GeoGit source code repository. To do so, create a new folder where you want the GeoGit source code to be kept, open a console and move to that folder. Now type the following in a console.

::

	$git clone http://github.com/opengeo/GeoGit.git

- Install the software needed to build GeoGit (you can skip one or both of the steps below in case you have Java and/or Maven already installed in your system)

	- Install the Java JDK from `this website <http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html>`_.

	- Install Maven following the instructions in `this website <http://maven.apache.org/download.cgi>`_.

- Move to the ``src/parent`` folder under the folder where you have cloned the GeoGit source code, and type the following.

::

	$mvn clean install

- GeoGit should now be built, and scripts ready to be run should be available in the `src/cli-app/target/geogit/bin` folder. Add that folders to your PATH environment variable, so your system can find it and you can call GeoGit from the console.

-To skip the tests during build process, use the following.

::

	$mvn clean install -DskipTests

-To test that GeoGit is ready to be used, open a console and type ``geogit help``. You should see a list of available commands like the one shown below.

::

	usage: geogit <command> [<args>]

	The most commonly used geogit commands are:
	--help          Print this help message, or provide a command name to get help for
	add             Add features to the staging area
	apply           Apply a patch to the current working tree
	branch          List, create, or delete branches
	cat             Provide content of an element in the repository
	checkout        Checkout a branch or paths to the working tree
	cherry-pick     Apply the changes introduced by existing commits
	clean           Deletes untracked features from working tree
	clone           Clone a repository into a new directory
	commit          Record staged changes to the repository
	config          Get and set repository or global options
	conflicts       Shows existing conflicts
	diff            Show changes between commits, commit and working tree, etc
	[...]
