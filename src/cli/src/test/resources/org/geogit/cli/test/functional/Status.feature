Feature: "status" command
    In order to know what I have staged and unstaged
    As a Geogit User
    I want to check the status of the current repository
     
  Scenario: Try to get the status of an empty directory
    Given I am in an empty directory
     When I run the command "status"
     Then the response should start with "Not a geogit repository"
     
  Scenario: Try to get the status of a repository with no changes
    Given I have a repository
     When I run the command "status"
     Then the response should contain "nothing to commit"
     
  Scenario: Try to get the status of a repository with unstaged changes without using a limit
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "status"
     Then the response should contain "Changes not staged for commit"
      And the response should contain "6 total."
      And the response should not contain "Changes to be committed"
      And the response should contain 12 lines
      
  Scenario: Try to get the status of a repository with staged changes without using a limit
    Given I have a repository
      And I stage 6 features
     When I run the command "status"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should not contain "Changes not staged for commit"
      And the response should contain 12 lines
      
  Scenario: Try to get the status of a repository with staged and unstaged changes without using a limit
    Given I have a repository
      And I stage 6 features
      And I modify a feature
     When I run the command "status"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should contain "Changes not staged for commit"
      And the response should contain "1 total."
      And the response should contain 18 lines
      
  Scenario: Try to get the status of a repository with unstaged changes specifying all
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "status --all"
     Then the response should contain "Changes not staged for commit"
      And the response should contain "6 total."
      And the response should not contain "Changes to be committed"
      And the response should contain 12 lines
      
  Scenario: Try to get the status of a repository with staged changes specifying all
    Given I have a repository
      And I stage 6 features
     When I run the command "status --all"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should not contain "Changes not staged for commit"
      And the response should contain 12 lines
      
  Scenario: Try to get the status of a repository with staged and unstaged changes specifying all
    Given I have a repository
      And I stage 6 features
      And I modify a feature
     When I run the command "status --all"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should contain "Changes not staged for commit"
      And the response should contain "1 total."
      And the response should contain 18 lines
      
   Scenario: Try to get the status of a repository with unstaged changes using a limit
    Given I have a repository
      And I have 6 unstaged features
     When I run the command "status --limit 3"
     Then the response should contain "Changes not staged for commit"
      And the response should contain "6 total."
      And the response should contain 9 lines

   Scenario: Try to get the status of a repository with staged changes using a limit
    Given I have a repository
      And I stage 6 features
     When I run the command "status --limit 3"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should contain 9 lines
      
  Scenario: Try to get the status of a repository with staged and unstaged changes using a limit
    Given I have a repository
      And I stage 6 features
      And I modify a feature
     When I run the command "status --limit 0"
     Then the response should contain "Changes to be committed"
      And the response should contain "6 total."
      And the response should contain "Changes not staged for commit"
      And the response should contain "1 total."
      And the response should contain 11 lines
      
   Scenario: Try to get the status of a repository with a negative limit
    Given I have a repository
     When I run the command "status --limit -2"
     Then the response should contain "Limit must be 0 or greater"
      
      
    
     
