Feature: "push" command
    In order to share my changes with a remote repository
    As a Geogit User
    I want to push my commits to that remote
     
  Scenario: Try to push from an empty directory
    Given I am in an empty directory
     When I run the command "push origin"
     Then the response should start with "Not in a geogit repository"
     
  Scenario: Try to push to origin
    Given I am in an empty directory
      And there is a remote repository
     When I run the command "clone remoterepo localrepo"
     Then the response should contain "Cloning into 'localrepo'..."
      And the response should contain "Done."
      And if I change to the respository subdirectory "localrepo"
     When I modify and add a feature
      And I run the command "commit -m Commit6"
      And I run the command "push"
     Then it should answer ""