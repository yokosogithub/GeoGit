Feature: "merge" command
    In order to combine two or more geogit histories into one
    As a Geogit User
    I want to merge one or more commit histories into my current branch

  Scenario: Try to merge one branch to a parent branch
    Given I have a repository
      And I have several branches
     When I run the command "merge branch1 -m MergeMessage"
     Then the response should contain "2 features added"
     When I run the command "log"
     Then the response should contain "MergeMessage"
      And the response should contain "Commit5"
      And the response should contain "Commit1"
      And the response should not contain "Commit4"
      And the response should not contain "Commit3"
      And the response should not contain "Commit2"
      
  Scenario: Try to merge the same branch twice
    Given I have a repository
      And I have several branches
     When I run the command "merge branch1 -m MergeMessage"
     Then the response should contain "2 features added"
     When I run the command "merge branch1 -m MergeMessage2"
     Then the response should contain "Already up to date."
      
  Scenario: Try to merge without specifying any commits
    Given I have a repository
      And I have several branches
     When I run the command "merge -m MergeMessage"
     Then it should answer "No commits provided to merge."
      
  Scenario: Try to merge a nonexistant branch
    Given I have a repository
      And I have several branches
     When I run the command "merge nonexistant"
     Then the response should start with "Commit not found"
     
  Scenario: Try to merge from an empty directory
    Given I am in an empty directory
     When I run the command "merge branch1"
     Then the response should start with "Not a geogit repository"

  Scenario: Try to merge two conflicting branches
    Given I have a repository
      And I have two conflicting branches
     When I run the command "merge branch1"
     Then the response should contain "CONFLICT: Merge conflict in Points/Points.1"     
     
  Scenario: Try to merge two conflicting branches using --ours strategy
    Given I have a repository
      And I have two conflicting branches
     When I run the command "merge branch1 --ours"
     Then the response should contain "Merge branch refs/heads/branch1"   

  Scenario: Try to merge two conflicting branches using --ours and --theirs strategy
    Given I have a repository
      And I have two conflicting branches
     When I run the command "merge branch1 --ours --theirs"
     Then the response should contain "Cannot use both --ours and --theirs" 
     
  Scenario: Try to merge two conflicting branches using --theirs strategy
    Given I have a repository
      And I have two conflicting branches
     When I run the command "merge branch1 --theirs"
     Then the response should contain "Merge branch refs/heads/branch1"     
     
  Scenario: Try to abort a conflicted merge
    Given I have a repository
      And I have a merge conflict state
     When I run the command "merge branch1 --abort"
     Then the response should contain "Merge aborted succesfully"     
     
  Scenario: Try to abort when there is no conflict
    Given I have a repository
     When I run the command "merge branch1 --abort"
     Then the response should contain "There is no merge to abort"            