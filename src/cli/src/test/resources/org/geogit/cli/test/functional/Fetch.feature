Feature: "fetch" command
    In order to get changes from a remote repository
    As a Geogit User
    I want to fetch new objects and branches to my local machine
     
  Scenario: Try to fetch from an empty directory
    Given I am in an empty directory
     When I run the command "fetch origin"
     Then the response should start with "Not a geogit repository"
     
