Feature: "pull" command
    In order to integrate changes from a remote repository to my working branch
    As a Geogit User
    I want to pull all new commits from that repository
     
  Scenario: Try to pull from an empty directory
    Given I am in an empty directory
     When I run the command "pull origin"
     Then the response should start with "Not a geogit repository"
     
