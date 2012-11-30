Feature: "commit" command
    In order to export data in the working tree
    As a Geogit User
    I want to export to a shapefile

  Scenario: Try to commit current staged features
    Given I have a repository
      And I stage 6 features
      When I run the command "shp export Points d:\\points.shp"