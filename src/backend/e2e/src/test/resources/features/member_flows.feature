@e2e
Feature: Member gym usage flows

  @gym
  Scenario: Member enters and exits the gym
    Given A member is authenticated for usage flows
    And The baseline gym attendance is stored
    When The member enters the gym
    Then The gym attendance increases by 1
    When The member exits the gym
    Then The gym attendance returns to baseline

  @area
  Scenario: Member enters and exits an area
    Given A member is authenticated for usage flows
    And The baseline for area "cardio-area" is stored
    When The member enters area "cardio-area"
    Then The area count for "cardio-area" increases by 1
    When The member exits area "cardio-area"
    Then The area count for "cardio-area" returns to baseline

  @machine
  Scenario: Member occupies and releases a machine
    Given A member is authenticated for usage flows
    And Machine "treadmill-01" is available
    When The member starts a session on machine "treadmill-01"
    Then Machine "treadmill-01" is occupied
    When The member ends the session on machine "treadmill-01"
    Then Machine "treadmill-01" is free

  @journey
  Scenario: Complete journey from gym entry to gym exit
    Given A member is authenticated for usage flows
    And The baseline gym attendance is stored
    And The baseline for area "cardio-area" is stored
    And Machine "treadmill-01" is available
    When The member completes a full workout journey in area "cardio-area" using machine "treadmill-01"
    Then The gym attendance returns to baseline
    And The area count for "cardio-area" returns to baseline
    And Machine "treadmill-01" is free

