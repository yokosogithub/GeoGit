Feature: "ls-tree" command
    In order to know what is in a repository
    As a Geogit User
    I want to list the feature in the working tree 
     
Scenario: Try to show a list of features in the root tree recursively
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree -r"
     Then the response should contain "Points/Points.1"
     Then the response should contain "Lines/Lines.1"   
     
Scenario: Try to show a list of features in the root tree recursively including trees
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree -r -t"
     Then the response should contain "Points/Points.1"
     Then the response should contain "Lines/Lines.1"     
     Then the response should contain "TREE"              
     
Scenario: Try to show a list of features in the root tree non-recursively
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree"
     Then the response should not contain "TREE"
     Then the response should not contain "Points/Points.1"          

Scenario: Try to show a list of features in the root tree non-recursively, including trees
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree -t"
     Then the response should contain "TREE"
     Then the response should not contain "Points/Points.1"       

Scenario: Try to show a list of features in the root tree recursively, not including children
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree -d"
     Then the response should contain "TREE"
     Then the response should not contain "Points/Points.1"           

Scenario: Try to show a list of features in a path
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree Points"
     Then the response should not contain "TREE"
     Then the response should contain "Points/Points.1"    
     
     
Scenario: Try to show a list of features using STAGE_HEAD as non-recursively, including trees
    Given I have a repository
      And I stage 6 features
     When I run the command "ls-tree STAGE_HEAD -t"
	 Then the response should contain "TREE"
     Then the response should not contain "Points/Points.1" 

Scenario: Try to show a list of features using HEAD as origin, recursively, including trees
    Given I have a repository
      And I stage 6 features
      And I run the command "commit -m Test"
     When I run the command "ls-tree HEAD -t"
     Then the response should contain "Points"
     Then the response should contain "Lines"  
     Then the response should not contain "Points/Points.1" 

Scenario: Try to show a list of features using HEAD as origin, recursively
    Given I have a repository
      And I stage 6 features
      And I run the command "commit -m Test"
     When I run the command "ls-tree HEAD -r"
     Then the response should contain "Points/Points.1"
     Then the response should contain "Lines/Lines.1"  
     Then the response should not contain "TREE"
     
Scenario: Try to show a list of features in a path, using HEAD as origin
    Given I have a repository
      And I stage 6 features
      And I run the command "commit -m Test"
     When I run the command "ls-tree HEAD:Points"
     Then the response should contain "Points/Points.1" 
     Then the response should not contain "Lines/Lines.1"        
     
Scenario: Try to show a list from an empty directory
    Given I am in an empty directory
     When I run the command "ls-tree"
     Then the response should start with "Not a geogit repository"         