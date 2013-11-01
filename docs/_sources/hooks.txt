GeoGit Hooks
=============


GeoGit supports hooks that are executed before or after a given operation is run.
Hooks are written in a supported scripting language (currently only JavaScript), and stored in the ``hooks`` folder in the GeoGit repository folder (``.geogit``).

When an operation is executed, GeoGit searches for a corresponding hook to run before the actual operation is run. A pre-execution hook should have the prefix ``pre_`` and the name of the operation before which is to be executed, and the extension of the corresponding scripting language used. A Python hook to be run before executing a commit operation should be, hence, named ``pre_commit.py``.

Pre-execution commits can halt the normal execution of the operation, and thus be used to perform checks. To indicate that the operation shouldn't be executed, a ``CannotRunGeogitOperationException`` has to be thrown. Any other exception type is assumed to be due to an error in the script and will not block the execution of the operation.

All hooks have a global variable named ``params`` that can be used to check the current operation parameters. It is a map containing the actual values of fields in the object representing the operation. Those values represent the arguments used when calling the the operation was invoked. Map keys are field names. 

At the end of this section you will find a list of operations that support hooks, and the parameters used by each one of them.

Values in this map can be modified, to alter how the operation is executed. Changing a value in the map has the same effect as modifying the actual invocation of the operation to which the pre-execution hook is linked.

Here is an example of a Python pre-commit hook (it should be saved in a file named ``pre_commit.js``) that illustrates the mechanism explained above:

::

	# a simple hook that avoids commiting with very short messages or with capital letters

	exception = Packages.org.geogit.api.hooks.CannotRunGeogitOperationException;
	msg = params.get("message");
	if (msg.length() < 30){
		throw new exception("Commit messages must have at least 30 letters");
	}

	# message is long enough. Make sure it is in lower case
	params.put("message", msg.toLowerCase());


GeoGit repositories are initialized with a set of sample hooks. They are not active by default, but can be enabled by removing the ``.sample`` suffix from their filename. The above example is one of them, named ``pre_commit.js.sample``.

GeoGit also supports post-execution hooks. Post-execution hooks run after the operation has been executed, and work in a very similar way. However, they are not expected to throw exceptions, and in case they do, it does not affect anything. A post execution hook is named using the prefix ``post_`` instead of ``pre_`` and the name of the operation after which is to be executed, and the extension of the corresponding scripting language used.

Post execution hooks shoould be used to perform management tasks once the corresponding operation has been executed. for instance, a post-execution hook linked to the export operation (with the ``post_export`` filename) can be used to execute mantainance operations on the data exported from GeoGit, like building spatial indexes or vacuuming a PostGIS table whenever data is exported from GeoGit into it.

GeoGit also support git-like hooks, written as executable console scripts. They have the same naming as the hooks described above, but a different extension (or no extension at all). If GeoGit finds a hook corresponding to a given operation, but it doesn't have the extension of one of the supported scripting languages, it will try to execute it (so you should make sure the file can be executed). No parameters are passed as arguments to these scripts.

Pre-execution hooks written this way can also prevent the actual operation to be executed. If the exit code of the script is non-zero, the operation will not be run, having the same effect as throwing a ``CannotRunGeogitOperationException`` exception in the above Python example. 

Since hook files can be written in several scripting languages, you might have several valid hook files in the corresponding folder of your GeoGit repository. However, GeoGit expects only one of them to be present, so when one of them is found, the others are ignored. Use only one single hook file for each supported operation and instant (pre- or post-execution).


Supported hooks
-----------------

Geogit support hooks for the following operations:

- ``commit``

	Parameters:

		- ``message``: the commit message.
		- ``commiterName``: the name of the commiter.
		- ``commiterEmail``: the email of the commiter.

- ``rebase``

	Parameters:

		TODO:


- ``checkout``

	Parameters:

		TODO:

- ``apply`` (applying a patch)

	Parameters:

		TODO:

- ``osmimport``. For the osm import command.

	Parameters:

		TODO:

- ``import``. For all import commands (shp, pg and sl)

	Parameters:

		- ``all``: true if it should import all tables from the datastore. It is always true in the case of importing from shapefiles
		- ``table``: the name of the single table to import.  It equals ``null`` in the case of importing from shapefiles
		- ``dataStore``: the GeoTools datastore to import from

- ``export``	

	Parameters:

		``featureTypeName``: the path of the feature type to export
    	``featureStore``: an instance of ``Supplier<SimpleFeatureStore>`` containing the GeoTools feature store to export to


The GeoGit scripting API
-------------------------

When creating a hook, it might be necessary to access some of the functionalities that GeoGit implements. Some of the most common operations are exposed through the GeoGit scripting API. 

A global variable named ``geogit`` is available to access the GeoGIT API. It contains an instance of an object of type ``GeoGitAPI``, which wraps GeoGit operations and provides methods to easily access it. Check the API documentation for detailed information about its methods.

To illustrate the usage of this facade class, below is an example of a hook that prevents committing features with topologically incorrect geometries.

::

	Validator = Packages.com.vividsolutions.jts.operation.valid.IsValidOp;
	var features = geogit.getFeaturesToCommit(null, true);
	for (var i = 0; i < features.length; i++) {
		var feature = features[i];
		geom = feature.getDefaultGeometry();
		op = new Validator(geom) ;
		if (!op.isValid()){
			geogit.throwHookException(op.getValidationError().getMessage());
		}


More elaborate hooks can be created, making use of the Geogit API along with the GeoTools classes that GeoGit internally uses. This can be used to perform more advanced checkings and transformations, such as reprojecting geometries before importing then into the repository.

Also, GeoGit commands can be called from the script, using the ``run()`` method from the ``geogit`` object. It takes the name of the class with the command to call as the first parameter. the second parameter is a dict with the names and values of of the parameters needed by that command to be executed.

The following is an example hook that trigger an OSM unmapping operation whenever the ``mapped`` tree (which is supposed to contain mapped OSM data), is modified after a commit.

::

	var diffs = geogit.getFeaturesToCommit('mapped', false);
    if (diffs.length > 0){
    	var params = {"path" : "mapped"};
    	geogit.run("org.geogit.osm.internal.OSMUnmapOp", params);
    }

The above code should be placed in a file named ``post_commit.js``

The example hooks which are added to a GeoGit repository upon initialization should serve as a starting point for building new hooks and understanding the GeoGit hook mechanism.