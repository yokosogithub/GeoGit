Feature: "list" command
    In order to know all of the features available on a PostGIS database
    As a Geogit User
    I want to list all of the features

  Scenario: Try listing from an empty directory
    Given I am in an empty directory
     When I run the command "pg list" on the database
     Then the response should start with "Not a geogit repository:"
      
  Scenario: Try listing from a valid directory
    Given I have a repository
     When I run the command "pg list" on the database
     Then the response should contain "geogit_pg_test"

