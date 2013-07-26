Feature: "conflicts" command
    In order to know which features are conflicted 
    As a Geogit User
    I want to get a list of conflicted elements

  Scenario: Try to merge list conflicts
    Given I have a repository
      And I have conflicting branches
     When I run the command "merge branch1"
     And I run the command "conflicts"
     Then the response should contain "Ancestor"
      And the response should contain "Ours"
      Then the response should contain "Theirs"    
     
Scenario: Try to merge list conflicts showing diffs
    Given I have a repository
      And I have conflicting branches
     When I run the command "merge branch1"
     And I run the command "conflicts --diff"
     Then the response should contain "StringProp1_1 -> StringProp1_2"   
     
Scenario: Try to merge list conflicts showing only ids
    Given I have a repository
      And I have conflicting branches
     When I run the command "merge branch1"
     And I run the command "conflicts --ids-only"
     Then the response should contain 1 lines
     
Scenario: Try to merge list conflicts showing only ids and diffs
    Given I have a repository
      And I have conflicting branches
     When I run the command "merge branch1"
     And I run the command "conflicts --ids-only --diff"
     Then the response should contain "Cannot use --diff and --ids-only at the same time"    
     
Scenario: Try to merge list conflicts when no conflicts exist
    Given I have a repository
      And I have conflicting branches
     When I run the command "merge branch1"
     And I run the command "conflicts --ids-only --diff"
     Then the response should contain "No elements need merging"       
      