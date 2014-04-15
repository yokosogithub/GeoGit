Feature: "merge" command
    In order to know the branches in a geogit repository
    As a Geogit User
    I want list all references

  Scenario: List all references
    Given I have a repository
      And I have several branches
     When I run the command "show-ref"
     Then the response should contain 3 lines     
      And the response should contain "master"
      And the response should contain "branch1"
      And the response should contain "branch2"
      