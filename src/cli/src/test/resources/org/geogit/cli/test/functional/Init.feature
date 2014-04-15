Feature: "init" command
    In order to start versioning my spatial datasets
    As a repository Owner
    I want to create a new repository on a directory of my choice

  Scenario: Create repository in the current empty directory
    Given I am in an empty directory
     When I run the command "init"
     Then it should answer "Initialized empty Geogit repository in ${currentdir}/.geogit"
      And the repository directory shall exist

  Scenario: Create repository specifying initial configuration
    Given I am in an empty directory
     When I run the command "init --config foo.bar=baz"
     Then it should answer "Initialized empty Geogit repository in ${currentdir}/.geogit"
      And the repository directory shall exist
     When I run the command "config foo.bar"
     Then it should answer "baz"

  Scenario: Create repository specifying the target directory
    Given I am in an empty directory
     When I run the command "init roads"
     Then it should answer "Initialized empty Geogit repository in ${currentdir}/roads/.geogit"
      And if I change to the respository subdirectory "roads"
     Then the repository directory shall exist

  Scenario: Try to init a repository when already inside a repository
    Given I have a repository
     When I run the command "init"
     Then it should answer "Reinitialized existing Geogit repository in ${currentdir}/.geogit"
      And the repository directory shall exist

  Scenario: Try to init a repository from inside a repository subdirectory
    Given I have a repository
      And I am inside a repository subdirectory "topp/shapes"
     When I run the command "init"
     Then the response should start with "Reinitialized existing Geogit repository in"
      And the repository directory shall exist
    
