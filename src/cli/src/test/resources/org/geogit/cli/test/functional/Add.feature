Feature: "add" command
    In order to prepare for making a commit to the geogit repository
    As a Geogit User
    I want to stage my changes to the working tree

  Scenario: Try to add features to the index
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "add"
     Then the response should contain "6 features staged for commit"
     
  Scenario: Try to add a specific feature type to the index
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "add Points"
     Then the response should contain "3 features staged for commit"
     
  Scenario: Try to add a specific feature to the index
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "add Points/Points.1"
     Then the response should contain "1 features staged for commit"
     
  Scenario: Try to add from an empty directory
    Given I am in an empty directory
     When I run the command "add"
     Then the response should start with "Not a geogit repository"
     
  Scenario: Try to add when no changes have been made
    Given I have a repository
     When I run the command "add"
     Then the response should contain "No unstaged features"
    
     
