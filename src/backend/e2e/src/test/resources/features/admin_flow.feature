@auth
Feature: Admin authentication flow

  Scenario: Admin login and logout
    Given The admin has a valid gateway token
    When The admin sends valid credentials to login
    Then The login is accepted and tracked
    When The admin sends a logout request
    Then The logout is accepted
