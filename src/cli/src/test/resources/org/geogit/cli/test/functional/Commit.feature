Feature: "commit" command
    In order to finalize a set of changes that have been staged
    As a Geogit User
    I want to create a commit and add it to the repository

  Scenario: Try to commit current staged features
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m Test"
     Then the response should contain "6 features added"
     
  Scenario: Try to perform multiple commits
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m Test"
     Then the response should contain "6 features added"
     When I modify and add a feature
      And I run the command "commit -m Test2"
     Then the response should contain "1 changed"
     
  Scenario: Try to commit without providing a message
    Given I have a repository
      And I stage 6 features
     When I run the command "commit"
     Then it should answer "No commit message provided"
     
  Scenario: Try to commit from an empty directory
    Given I am in an empty directory
     When I run the command "commit -m Test"
     Then the response should start with "Not a geogit repository"
     
  Scenario: Try to commit when no changes have been made
    Given I have a repository
     When I run the command "commit -m Test"
     Then the response should start with "Nothing to commit"
     
