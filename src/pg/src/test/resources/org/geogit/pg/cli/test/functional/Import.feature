Feature: "import" command
    In order to import data to Geogit
    As a Geogit User
    I want to import one or more tables from a PostGIS database

  Scenario: Try importing into an empty directory
    Given I am in an empty directory
     When I run the command "pg import --table geogit_pg_test" on the database
     Then the response should start with "Not a geogit repository:"
      
  Scenario: Try to import a PostGIS table
    Given I have a repository
     When I run the command "pg import --table geogit_pg_test" on the database
     Then the response should contain "100%"

  Scenario: Try to import a full PostGIS database
    Given I have a repository
     When I run the command "pg import --all" on the database
     Then the response should contain "100%"
     
  Scenario: Try to import a PostGIS table that doesn't exit in the database
    Given I have a repository
     When I run the command "pg import --table nonexistant_table" on the database
     Then the response should contain "Could not find the specified table."
