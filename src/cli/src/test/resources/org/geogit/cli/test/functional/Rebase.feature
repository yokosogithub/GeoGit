Feature: "rebase" command
    In order to linearize the geogit history
    As a Geogit User
    I want to rebase my local commits onto an existing branch

  Scenario: Try to rebase one branch to a parent branch
    Given I have a repository
      And I have several branches
     When I run the command "rebase master branch1"
      And I run the command "log"
     Then the response should contain "Commit1"
      And the response should contain "Commit2"
      And the response should contain "Commit3"
      And the response should not contain "Commit4"
      And the response should contain "Commit5"
      
  Scenario: Try to graft a branch onto another branch
    Given I have a repository
      And I have several branches
     When I run the command "rebase branch1 branch2 --onto master"
      And I run the command "log"
     Then the response should contain "Commit1"
      And the response should not contain "Commit2"
      And the response should not contain "Commit3"
      And the response should contain "Commit4"
      And the response should contain "Commit5"
      
  Scenario: Try to rebase a nonexistant branch
    Given I have a repository
      And I have several branches
     When I run the command "rebase master nonexistant"
     Then it should answer "The branch reference could not be resolved."
     
  Scenario: Try to rebase to a nonexistant upstream
    Given I have a repository
      And I have several branches
     When I run the command "rebase nonexistant branch1"
     Then it should answer "The upstream reference could not be resolved."
     
  Scenario: Try to graft a branch onto a nonexistant branch
    Given I have a repository
      And I have several branches
     When I run the command "rebase master branch1 --onto nonexistant"
     Then it should answer "The onto reference could not be resolved."
     
  Scenario: Try to rebase from an empty directory
    Given I am in an empty directory
     When I run the command "rebase master branch1"
     Then it should answer "Not in a geogit repository."
     
