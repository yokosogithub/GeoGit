Feature: "fetch" command
    In order to get changes from a remote repository
    As a Geogit User
    I want to fetch new objects and branches to my local machine
     
  Scenario: Try to fetch from an empty directory
    Given I am in an empty directory
     When I run the command "fetch origin"
     Then the response should start with "Not a geogit repository"
     
  Scenario: Try to fetch from origin
    Given I have a repository with a remote
     When I run the command "fetch origin"
      And I run the command "branch --all"
     Then the response should contain "origin/master"
      And the response should contain "origin/branch1"
      And the response should contain "origin/HEAD"
      
  Scenario: Try to fetch the full history for a shallow clone
    Given I am in an empty directory
      And there is a remote repository
     When I run the command "clone --depth 1 remoterepo localrepo"
      And I run the command "log"
     Then the response should not contain "Commit4"
     When I run the command "fetch --fulldepth"
      And I run the command "log"
     Then the response should contain "Commit5"
      And the response should contain "Commit4"
      And the response should not contain "Commit3"
      And the response should not contain "Commit2"
      And the response should contain "Commit1"
      
  Scenario: Try to deepen the history of a shallow clone
    Given I am in an empty directory
      And there is a remote repository
     When I run the command "clone --depth 1 remoterepo localrepo"
      And I run the command "log"
     Then the response should not contain "Commit4"
     When I run the command "fetch --depth 2"
      And I run the command "log"
     Then the response should contain "Commit5"
      And the response should contain "Commit4"
      And the response should not contain "Commit1"
      
  Scenario: Try to fetch from origin without specifying a remote
    Given I have a repository with a remote
     When I run the command "fetch"
      And I run the command "branch --all"
     Then the response should contain "origin/master"
      And the response should contain "origin/branch1"
      And the response should contain "origin/HEAD"
      
  Scenario: Try to fetch from an invalid remote
    Given I have a repository
     When I run the command "fetch origin"
     Then it should answer "Remote could not be resolved."
     
  Scenario: Try to fetch from origin with pruning
    Given I have a repository with a remote
      And I have a remote ref called "branch2"
     When I run the command "branch --all"
     Then the response should contain "origin/branch2"
     When I run the command "fetch --prune"
      And I run the command "branch --all"
     Then the response should contain "origin/master"
      And the response should contain "origin/branch1"
      And the response should not contain "origin/branch2"
      And the response should contain "origin/HEAD"
