Feature: "clone" command
    In order to build on the work in an existing repository
    As a Geogit User
    I want to clone that repository to my local machine
     
  Scenario: Try to clone without specifying a repository
    Given I am in an empty directory
     When I run the command "clone"
     Then it should answer "You must specify a repository to clone."
     
  Scenario: Try to clone with too many parameters
    Given I am in an empty directory
     When I run the command "clone repository directory extra"
     Then it should answer "Too many arguments provided."
