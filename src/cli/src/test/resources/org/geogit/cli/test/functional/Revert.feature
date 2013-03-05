Feature: "revert" command
	In order to undo committed changes
	As a Geogit user
	I want to revert a series of commits and commit those changes
	
  Scenario: Try to revert something while not in a geogit repository
  	Given I am in an empty directory
  	  And I run the command "revert master"
  	 Then the response should contain "not in a geogit repository"
  	 
  Scenario: Try to revert with nothing specified for reverting
    Given I have a repository
      And I run the command "revert"
     Then the response should contain "nothing specified for reverting"
     
  Scenario: Try to revert one commit
    Given I have a repository
      And I have several commits
     When I run the command "revert master"
      And I run the command "log"
     Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      And the response should contain "Subject: Revert of commit"
      
  Scenario: Try to revert a commit that doesn't exist
    Given I have a repository
      And I have several commits
     When I run the command "revert doesntExist"
     Then the response should contain "Couldn't resolve 'doesntExist' to a commit, aborting revert"
     When I run the command "log"
     Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      And the response should not contain "Subject: Revert of commit"
     
  Scenario: Try to revert multiple commits
   Given I have a repository
     And I have several commits
    When I run the command "revert master~1 master~2"
     And I run the command "log"
     Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      And the response should contain "Subject: Revert of commit"
      
  Scenario: Try to revert multiple commits but with one nonexistant commit
   Given I have a repository
     And I have several commits
    When I run the command "revert master~1 blah"
    Then the response should contain "Couldn't resolve 'blah' to a commit, aborting revert"
    When I run the command "log"
    Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      And the response should not contain "Subject: Revert of commit"