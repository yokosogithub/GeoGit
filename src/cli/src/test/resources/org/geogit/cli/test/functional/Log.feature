Feature: "log" command
    In order to know the history of commits on a repository
    As a Geogit User
    I want to log them to the console

  Scenario: Try to show a log of a repository with a single commit.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     Then the response should contain "6 features added"
     When I run the command "log"
     Then the response should contain "Subject: TestCommit"
     
  Scenario: Try to show a log of a repository with several commits.
    Given I have a repository
      And I have several commits
      And I run the command "log"
     Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      
  Scenario: Try to show only the last two commits.
    Given I have a repository
      And I have several commits
      And I run the command "log -n 2"
     Then the response should not contain "Subject: Commit1"
      And the response should not contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should contain "Subject: Commit4"
      
  Scenario: Try to show the log, skipping the last 2 commits
    Given I have a repository
      And I have several commits
      And I run the command "log --skip 2"
     Then the response should contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should not contain "Subject: Commit3"
      And the response should not contain "Subject: Commit4"
      
  Scenario: Try to show the last 2 commits before the most recent
    Given I have a repository
      And I have several commits
      And I run the command "log -n 2 --skip 1"
     Then the response should not contain "Subject: Commit1"
      And the response should contain "Subject: Commit2"
      And the response should contain "Subject: Commit3"
      And the response should not contain "Subject: Commit4"
      
  Scenario: Try to show a log from an empty directory
    Given I am in an empty directory
     When I run the command "log"
     Then the response should start with "Not a geogit repository"
      