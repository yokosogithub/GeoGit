Web-Api Documentation
==============================

This will walk you through GeoGit's web-api and all of its currently supported functionality. This doc also assumes that you already know what any given command does, this is strictly how to use these commands. First I will explain an easy way to get the web-api up and running.

If you don't already have the GeoGit source from GitHub and a GeoGit repository set up do that first. Next, to get the web-api up and running after you have built the latest GeoGit go into the web/app folder in the GeoGit source and run this command: 
``mvn exec:java -Dexec.mainClass=org.geogit.web.Main -Dexec.args=/Path/To/Repo``. This will set up a jetty server with your repository and give you access to the web-api. Now you just need to open up a web browser and go to localhost:8182/log to make sure it comes up. After you have gotten it up and running now you can test any of the commands listed here.

All web-api commands have transaction support, which means you can run the web-api commands beginTransaction to start a transaction and then use the id that is returned to do other commands on that transaction instead of the actual repository. After you are done with everything on the transaction you just have call endTransaction through the web-api and pass it the transaction id. Some commands require a transaction to preserve the stability of the repository, those that require it will have ``(-T)`` next to the name of the command in this doc. To pass the transaction id you just need to use the transactionId option and set it equal to the id that beginTransaction returns. Some commands also have other options that required for that command to work they will be italicized in this doc. Any options that have notes associated with them have and asterisk next to them.

.. note:: All web-api command response are formatted for xml by default, however you can get a JSON response by adding this option to the url ``output_format=JSON``.

Porcelain Commands Supported
-----------------------------------------------

- Add (-T)

	 Currently Supported Options:
		a) *path* - the path to the feature you want to add
			Type: String
			
			Default: null
			
 ::

	Example: localhost:8182/add?path=tree/fid&transactionId=id
	
- Branch

	 Currently Supported Options:
		a) *list* - true to list any branches
			Type: Boolean
			
			Default: false
		b) remotes - true to list remote branches
			Type: Boolean
			
			Default: false

 ::

	Example: localhost:8182/branch?list=true&remote=true

- Checkout (-T)

	 Currently Supported Options:
		a) *branch** - the name of the branch to checkout
			Type: String
			
			Default: null
		b) *ours** - true to use our version of the feature specified
			Type: Boolean
			
			Default: false
		c) *theirs** - true to use their version of the feature specified
			Type: Boolean
			
			Default: false
		d) *path** - the path to the feature that will be updated
			Type: String
			
			Default: null

 .. note:: You must specify either branch OR path not both. If path is specified then you MUST specify either ours or theirs.

 ::

	Examples: localhost:8182/checkout?branch=master&transactionId=id
	       	  localhost:8182/checkout?path=tree/fid&ours=true&transactionId=id

- Commit (-T)

	Currently Supported Options:
		a) message - the message for this commit
			Type: String
			
			Default: null
		b) all - true to commit everything in the working tree
			Type: Boolean
			
			Default: false
		c) authorName - the author of the commit
			Type: String
			
			Default: null
		d) authorEmail - the email of the the author of the commit
			Type: String
			
			Default: null

 ::

	Example: localhost:8182/commit?authorName=John&authorEmail=john@example.com&message=something&all=true&transactionId=id

- Diff

	Currently Supported Options:
		a) *oldRefSpec* - the old ref spec to diff against
			Type: String
			
			Default: null
		b) newRefSpec - the new ref spec to diff against
			Type: String
			
			Default: null
		c) pathFilter - a path to filter by
			Type: String
			
			Default: null
		d) showGeometryChanges - true to show geometry changes
			Type: Boolean
			
			Default: false
		e) page - the page number to build the response
			Type: Integer
			
			Default: 0
		f) show - the number of elements to display in the response per page
			Type: Integer
			
			Default: 30

 ::

	Example: localhost:8182/diff?oldRefSpec=commitId1&newRefSpec=commitId2&showGeometryChanges=true&show=100

- Fetch

	Currently Supported Options:
		a) prune - true to prune remote tracking branches locally that no longer exist
			Type: Boolean
			
			Default: false
		b) all - true to fetch from all remotes
			Type: Boolean
			
			Default: false
		c) *remote** - the remote to fetch from
			Type: String
			
			Default: origin

 .. note:: If remote is not specified it will try to fetch from a remote named origin.

 ::

	Example: localhost:8182/fetch?prune=true&remote=origin

- Log

	Currently Supported Options:
		a) limit - the number of commits to print
			Type: Integer
			
			Default: null
		b) offset - the offset to start listing at
			Type: Integer
			
			Default: null
		c) path - a list of paths to filter commits by
			Type: List<String>
			
			Default: Empty List
		d) since - the start place to list commits
			Type: String
			
			Default: null
		e) until - the end place to list commits
			Type: String
			
			Default: null
		f) page - the page number to build the response
			Type: Integer
			
			Default: 0
		g) show - the number of elements to display in the response per page
			Type: Integer
			
			Default: 30
		h) firstParentOnly - true to only show the first parent of a commit
			Type: Boolean
			
			Default: false

 ::

	Example: localhost:8182/log?path=treeName&firstParentOnly=true

- Merge (-T)

	Currently Supported Options:
		a) noCommit - true to merge without creating a commit afterwards
			Type: Boolean
			
			Default: false
		b) *commit** - the commit to merge into the currently checked out ref
			Type: String
			
			Default: null
		c) authorName - the author of the merge commit
			Type: String
			
			Default: null
		d) authorEmail - the email of the author of the merge commit
			Type: String
			
			Default: null

 .. note:: You can also pass a ref name for the commit option, instead of a commit hash.

 ::

	Example: localhost:8182/merge?commit=branch1&noCommit=true&transactionId=id

- Pull

	Currently Supported Options:
		a) *remoteName** - the name of the remote to pull from
			Type: String
			
			Default: origin
		b) all - true to fetch all
			Type: Boolean
			
			Default: false
		c) *ref** - the ref to pull
			Type: String
			
			Default: Currently Checked Out Branch
		d) authorName - the author of the merge commit
			Type: String
			
			Default: null
		e) authorEmail - the email of the author of the merge commit
			Type: String
			
			Default: null

 .. note:: If you don't specify the remoteName it will try to pull from a remote named   origin. Also, if ref is not specified it will try to pull the currently checked out branch. The ref option should be in this format remoteref:localref, with the localref portion being optional. If you should opt out of specifying the localref it will just use the same name as the remoteref.

 ::

	Example: localhost:8182/pull?remoteName=origin&all=true&ref=master:master

- Push

	Currently Supported Options:
		a) all - true to push all refs
			Type: Boolean
			
			Default: false
		b) *ref** - the ref to push
			Type: String
			
			Default: Currently Checked Out Branch
		c) *remoteName** - the name of the remote to push to
			Type: String
			
			Default: origin

 .. note:: If you don't specify the remoteName it will try to push to a remote named origin. Also, if ref is not specified it will try to push the currently checked out branch. The ref option should be in this format localref:remoteref, with the remoteref portion being optional. If you should opt out of specifying the remoteref it will just use the same name as the localref.

 ::

	Example: localhost:8182/push?ref=master:master&remoteName=origin

- Remote

	Currently Supported Options:
		a) *list** - true to list the names of your remotes
			Type: Boolean
			
			Default: false
		b) remove - true to remove the given remote
			Type: Boolean
			
			Default: false
		c) *remoteName** - the name of the remote to add or remove
			Type: String
			
			Default: null
		d) remoteURL - the URL to the repo to make a remote
			Type: String
			
			Default: null

 .. note:: You must specify either list OR remoteName for the command to work. If remoteName is specified but remove is false then remoteURL is required as well.

 ::

	Examples: localhost:8182/remote?list=true
	          localhost:8182/remote?remove=true&remoteName=origin
	       	  localhost:8182/remote?remoteName=origin&remoteURL=urlToRepo.com

- Remove (-T)

	Currently Supported Options:
		a) *path* - the path to the feature to be removed
			Type: String
			
			Default: null
		b) recursive - true to remove a tree and all features under it
			Type: Boolean
			
			Default: false

 ::

	Examples: localhost:8182/remove?path=treeName/fid&transactionId=id
	       	  localhost:8182/remove?path=treeName&recursive=true&transactionId=id

- Status

	Currently Supported Options:
		a) limit - the number of staged and unstaged changes to make
			Type: Integer
			
			Default: 50
		b) offset - the offset to start listing staged and unstaged changes
			Type: Integer
			
			Default: 0


 ::

	Example: localhost:8182/status?limit=100

- Tag

	Currently Supported Options:
		a) *list* - true to list the names of your tags
			Type: Boolean
			
			Default: false

 ::

	Example: localhost:8182/tag?list=true

- Version

	Currently Supported Options:
		none

 ::

	Example: localhost:8182/version

Plumbing Commands Supported
-------------------------------------------------------

- BeginTransaction

	Currently Supported Options:
		none

 ::

	Example: localhost:8182/beginTransaction

- EndTransaction (-T)

	Currently Supported Options:
		a) cancel - true to abort all changes made in this transaction
			Type: Boolean
			
			Default: false

 ::

	Example: localhost:8182/endTransaction?cancel=true&transactionId=id

- FeatureDiff

	Currently Supported Options:
		a) *path* - the path to feature
			Type: String
			
			Default: null
		b) *newCommitId** - the id of the newer commit
			Type: String
			
			Default: ObjectId.NULL
		c) *oldCommitId** - the id of the older commit
			Type: String
			
			Default: ObjectId.NULL
		d) all - true to show all attributes not just changed ones
			Type: Boolean
			
			Default: false

 .. note:: If no newCommitId is specified then it will use the commit that HEAD is pointing to. If no oldCommitId is specified then it will assume you want the diff to include the initial commit.

 ::

	Example: localhost:8182/featurediff?path=treeName/fid&newCommitId=commitId1&oldCommitId=commitId2

- LsTree

	Currently Supported Options:
		a) showTree - true to display trees in the response
			Type: Boolean
			
			Default: false
		b) onlyTree - true to display only trees in the response
			Type: Boolean
			
			Default: false
		c) recursive - true to recurse through the trees
			Type: Boolean
			
			Default: false
		d) verbose - true to print out the type, metadataId and Id of the object
			Type: Boolean
			
			Default: false
		e) *path** - reference to start at
			Type: String
			
			Default: null

 .. note:: If path is not specified it will use the WORK_HEAD.

 ::

	Example: localhost:8182/ls-tree?showTree=true&recursive=true&verbose=true

- RefParse

	Currently Supported Options:
		a) *name* - the name of the ref to parse
			Type: String
			
			Default: null

 ::

	Example: localhost:8182/refparse?name=master

- UpdateRef

	Currently Supported Options:
		a) *name* - the name of the ref to update
			Type: String
			
			Default: null
		b) *delete** - true to delete this ref
			Type: Boolean
			
			Default: false
		c) *newValue** - the new value to change the ref to
			Type: String
			
			Default: ObjectId.NULL

 .. note:: You must specify either delete OR newValue for the command to work.

 ::

	Example: localhost:8182/updateref?name=master&newValue=branch1

Web-Api Specific
-----------------------------

There is currently only one web-api specific function at this time and it is to traverse the entire commit graph. It starts at the specified commitId and works its way down the graph to either the initial commit or the specified depth. Since it traverses the actual commit graph, unlike log, it will display multiple parents and will list every single commit that runs down each parents history.

- GetCommitGraph

	Currently Supported Options:
		a) depth - the depth to search to
			Type: Integer
			
			Default: 0
		b) *commitId* - the id of the commit to start at
			Type: String
			
			Default: ObjectId.NULL
		c) page - the page number to build the response
			Type: Integer
			
			Default: 0
		d) show - the number of elements to list per page
			Type: Integer
			
			Default: 30

 ::

	Example: localhost:8182/getCommitGraph?show=100

Issues
=======

The main concern with the web-api currently is that it doesn't have any kind of authentication on it, which means that anyone with the url can potentially destroy your repo or steal you data with commands like updateref and pull.

There is also a lot of room for improvement and optimization. There are also several commands that still need to be exposed through the web-api. 
