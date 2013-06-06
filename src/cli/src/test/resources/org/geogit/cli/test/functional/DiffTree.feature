Feature: "diff-tree" command
    In order to know changes made in a repository
    As a Geogit User
    I want to see the existing differences between commits 

Scenario: Show diff between working tree and index
    Given I have a repository
      And I stage 6 features      
      And I modify a feature
     When I run the command "diff-tree"
     Then the response should contain "Points/Points.1"       
      And the response should contain 1 lines
      
Scenario: Show diff using too many commit refspecs
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff-tree commit1 commit2 commit3"
	 Then the response should contain "Commit list is too long"  
	  And it should exit with non-zero exit code 
	 
Scenario: Show diff using a wrong commit refspec
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff-tree wrongcommit"
	 Then the response should contain "Refspec wrongcommit does not resolve to a tree"
	  And it should exit with non-zero exit code  	 
         
Scenario: Show diff between working tree and index, describing the modified element
    Given I have a repository
      And I stage 6 features   
      And I modify a feature       
     When I run the command "diff-tree -- Points/Points.1 --describe"
     Then the response should contain 10 lines     

Scenario: Show diff between working tree and index, describing the modified element, with a change in the feature type
    Given I have a repository
      And I stage 6 features   
      And I modify a feature type       
     When I run the command "diff-tree -- Points/Points.1 --describe"
     Then the response should contain 9 lines   
       
                        
     