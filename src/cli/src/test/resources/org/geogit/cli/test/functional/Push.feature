Feature: "push" command
    In order to share my changes with a remote repository
    As a Geogit User
    I want to push my commits to that remote
     
  Scenario: Try to push from an empty directory
    Given I am in an empty directory
     When I run the command "push origin"
     Then the response should start with "Not a geogit repository"
     
