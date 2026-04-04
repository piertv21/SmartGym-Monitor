@auth
Feature: Admin authentication flow

  Scenario: Admin login and logout
    Given The admin credentials are valid
    When The admin sends valid credentials to login
    Then The login is accepted and tracked
    When The admin sends a logout request with the access token
    Then The logout is accepted
