Feature: "cherry-pick" command
    In order to select specific changes to bring to the current branch
    As a Geogit User
    I want to cherry pick several commits from other branches

  Scenario: Try to cherry pick several commits
    Given I have a repository
      And I have several branches
     When I run the command "cherry-pick branch1 branch2"
      And I run the command "log"
     Then the response should contain "Commit4"
      And the response should contain "Commit3"
      And the response should contain "Commit5"
      And the response should not contain "Commit2"
      And the response should contain "Commit1"
      
  Scenario: Try to cherry pick a single commit
    Given I have a repository
      And I have several branches
     When I run the command "cherry-pick branch1"
      And I run the command "log"
     Then the response should contain "Commit3"
      And the response should not contain "Commit2"
      And the response should not contain "Commit4"
      And the response should contain "Commit5"
      And the response should contain "Commit1"
      
  Scenario: Try to cherry pick a nonexistant branch
    Given I have a repository
      And I have several branches
     When I run the command "cherry-pick nonexistant"
     Then the response should contain "Commit not found"
     
  Scenario: Try to cherry pick without specifying any commits
    Given I have a repository
      And I have several branches
     When I run the command "cherry-pick"
     Then it should answer "No commits specified."
     
  Scenario: Try to cherry pick from an empty directory
    Given I am in an empty directory
     When I run the command "cherry-pick branch1"
     Then it should answer "Not in a geogit repository."
     
