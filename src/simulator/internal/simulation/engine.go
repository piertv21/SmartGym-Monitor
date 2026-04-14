package simulation

import (
	"fmt"
	"sync"
	"time"

	"smartgym-monitor/simulator/internal/mqtt"
)

// ---------------------------------------------------------------------------
// Timing constants
// ---------------------------------------------------------------------------

const (
	// Gym-level timing.
	minOutsideWaitSeconds = 10
	maxOutsideWaitSeconds = 30
	minLobbyStaySeconds   = 5
	maxLobbyStaySeconds   = 15
	minLeavingStaySeconds = 5
	maxLeavingStaySeconds = 10

	// Area-level timing (areas WITHOUT machines).
	minNoMachineStaySeconds = 10
	maxNoMachineStaySeconds = 30

	// Machine session timing.
	minMachineSessionSeconds = 10
	maxMachineSessionSeconds = 40

	// How many machines a user will use per area (with machines).
	minMachinesPerArea = 1
	maxMachinesPerArea = 3

	// Workout plan length (number of areas to visit, excluding entrance).
	minPlanLength = 2
	maxPlanLength = 4
)

// ---------------------------------------------------------------------------
// Member state enum
// ---------------------------------------------------------------------------

type MemberState string

const (
	StateOutside      MemberState = "OUTSIDE"
	StateInLobby      MemberState = "IN_LOBBY"
	StateInArea       MemberState = "IN_AREA"
	StateUsingMachine MemberState = "USING_MACHINE"
	StateLeaving      MemberState = "LEAVING"
)

// ---------------------------------------------------------------------------
// MQTT message types
// ---------------------------------------------------------------------------

type GymAccessMessage struct {
	DeviceID   string `json:"deviceId"`
	Timestamp  string `json:"timeStamp"`
	BadgeID    string `json:"badgeId"`
	AccessType string `json:"accessType"`
}

type AreaAccessMessage struct {
	DeviceID  string `json:"deviceId"`
	Timestamp string `json:"timeStamp"`
	BadgeID   string `json:"badgeId"`
	AreaID    string `json:"areaId"`
	Direction string `json:"direction"`
}

type MachineUsageMessage struct {
	DeviceID   string `json:"deviceId"`
	Timestamp  string `json:"timeStamp"`
	MachineID  string `json:"machineId"`
	BadgeID    string `json:"badgeId"`
	UsageState string `json:"usageState"`
}

type DeviceStatusMessage struct {
	DeviceID     string `json:"deviceId"`
	Timestamp    string `json:"timeStamp"`
	DeviceType   string `json:"deviceType"`
	Online       bool   `json:"online"`
	StatusDetail string `json:"statusDetail"`
}

// ---------------------------------------------------------------------------
// Domain models
// ---------------------------------------------------------------------------

type GymMember struct {
	BadgeID string
	State   MemberState

	// Timing: when the current phase started / when the next transition fires.
	PhaseStart     time.Time
	NextTransition time.Time

	// Which area the member is currently in (valid for IN_LOBBY, IN_AREA,
	// USING_MACHINE, LEAVING).
	CurrentAreaID string

	// Machine usage (valid only in USING_MACHINE state).
	UsingMachineID   string
	MachinesUsedHere int // how many machines used so far in the current area
	MachinesTarget   int // how many machines to use before leaving the area

	// Workout plan: ordered list of area IDs to visit (never includes "entrance-area").
	WorkoutPlan      []string
	CurrentPlanIndex int
}

type Machine struct {
	MachineID      string
	SensorID       string
	AreaID         string
	Occupied       bool
	CurrentBadgeID string
	SessionEndTime time.Time
}

type Area struct {
	AreaID      string
	ReaderID    string
	HasMachines bool
}

type Device struct {
	DeviceID    string
	DeviceType  string
	Online      bool
	FailRate    float64
	RecoverRate float64
}

type RNG interface {
	Float64() float64
	Intn(int) int
	Shuffle(n int, swap func(i, j int))
}

// ---------------------------------------------------------------------------
// Engine
// ---------------------------------------------------------------------------

type Engine struct {
	mu        sync.Mutex
	publisher mqtt.Publisher
	rng       RNG
	areas     []Area
	machines  []Machine
	devices   []Device
	members   []GymMember
}

func NewEngine(publisher mqtt.Publisher, rng RNG) *Engine {
	areas := buildAreas()
	machines := buildMachines()
	devices := buildDevices(areas, machines)

	members := make([]GymMember, 0, 15)
	for i := 1; i <= 15; i++ {
		members = append(members, GymMember{
			BadgeID: badgeID(i),
			State:   StateOutside,
		})
	}

	return &Engine{
		publisher: publisher,
		rng:       rng,
		areas:     areas,
		machines:  machines,
		devices:   devices,
		members:   members,
	}
}

func (e *Engine) Counts() (areas, machines, devices, members int) {
	e.mu.Lock()
	defer e.mu.Unlock()
	return len(e.areas), len(e.machines), len(e.devices), len(e.members)
}

// ---------------------------------------------------------------------------
// BusinessStep — one tick of the simulation
// ---------------------------------------------------------------------------

func (e *Engine) BusinessStep(now time.Time) {
	e.mu.Lock()
	defer e.mu.Unlock()

	for i := range e.members {
		m := &e.members[i]
		e.stepMember(m, now)
	}
}

func (e *Engine) stepMember(m *GymMember, now time.Time) {
	switch m.State {
	case StateOutside:
		e.stepOutside(m, now)
	case StateInLobby:
		e.stepInLobby(m, now)
	case StateInArea:
		e.stepInArea(m, now)
	case StateUsingMachine:
		e.stepUsingMachine(m, now)
	case StateLeaving:
		e.stepLeaving(m, now)
	}
}

// ---------------------------------------------------------------------------
// State: OUTSIDE — user is outside, waiting to enter the gym
// ---------------------------------------------------------------------------

func (e *Engine) stepOutside(m *GymMember, now time.Time) {
	// First-time initialization: schedule when the user will enter.
	if m.NextTransition.IsZero() {
		m.NextTransition = now.Add(randomDurationBetween(e.rng, minOutsideWaitSeconds, maxOutsideWaitSeconds))
		return
	}

	if now.Before(m.NextTransition) {
		return
	}

	// Enter the gym.
	e.publisher.PublishJSON("gym-access", GymAccessMessage{
		DeviceID:   "turnstile-entry-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    m.BadgeID,
		AccessType: "ENTRY",
	})

	// Enter the "entrance-area" area immediately.
	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  e.findAreaReader("entrance-area"),
		Timestamp: currentTimestamp(now),
		BadgeID:   m.BadgeID,
		AreaID:    "entrance-area",
		Direction: "IN",
	})

	// Build a workout plan.
	m.WorkoutPlan = e.buildWorkoutPlan()
	m.CurrentPlanIndex = 0

	m.State = StateInLobby
	m.CurrentAreaID = "entrance-area"
	m.PhaseStart = now
	m.NextTransition = now.Add(randomDurationBetween(e.rng, minLobbyStaySeconds, maxLobbyStaySeconds))
	m.UsingMachineID = ""
	m.MachinesUsedHere = 0
	m.MachinesTarget = 0
}

// ---------------------------------------------------------------------------
// State: IN_LOBBY — user is in "entrance-area", about to start workout
// ---------------------------------------------------------------------------

func (e *Engine) stepInLobby(m *GymMember, now time.Time) {
	if now.Before(m.NextTransition) {
		return
	}

	if m.CurrentPlanIndex >= len(m.WorkoutPlan) {
		// No areas to visit (shouldn't normally happen, but be safe).
		e.transitionToLeaving(m, now)
		return
	}

	// Move from entrance to the first area of the plan.
	nextAreaID := m.WorkoutPlan[m.CurrentPlanIndex]
	e.transitArea(m, "entrance-area", nextAreaID, now)
	m.CurrentPlanIndex++

	e.enterArea(m, nextAreaID, now)
}

// ---------------------------------------------------------------------------
// State: IN_AREA — user is in an area (with or without machines)
// ---------------------------------------------------------------------------

func (e *Engine) stepInArea(m *GymMember, now time.Time) {
	area := e.findArea(m.CurrentAreaID)
	if area == nil {
		// Shouldn't happen; recover by going back to entrance.
		e.transitionToLeaving(m, now)
		return
	}

	if area.HasMachines {
		e.stepInAreaWithMachines(m, now)
	} else {
		e.stepInAreaWithoutMachines(m, now)
	}
}

func (e *Engine) stepInAreaWithoutMachines(m *GymMember, now time.Time) {
	// Stay for the predetermined duration, then move on.
	if now.Before(m.NextTransition) {
		return
	}
	e.leaveCurrentArea(m, now)
}

func (e *Engine) stepInAreaWithMachines(m *GymMember, now time.Time) {
	// The user hasn't started any machine yet, or has finished one and
	// still has more to go.
	if m.MachinesUsedHere >= m.MachinesTarget {
		// Done with all machines in this area — leave.
		e.leaveCurrentArea(m, now)
		return
	}

	// Wait a bit after entering before starting a machine.
	if now.Sub(m.PhaseStart) < 10*time.Second {
		return
	}

	// Try to find a free machine.
	machine := e.findFreeMachineInArea(m.CurrentAreaID)
	if machine == nil {
		// No machine available — leave the area regardless.
		e.leaveCurrentArea(m, now)
		return
	}

	// Start using the machine.
	machine.Occupied = true
	machine.CurrentBadgeID = m.BadgeID
	machine.SessionEndTime = now.Add(randomDurationBetween(e.rng, minMachineSessionSeconds, maxMachineSessionSeconds))
	m.UsingMachineID = machine.MachineID
	m.State = StateUsingMachine

	e.publisher.PublishJSON("machine-usage", MachineUsageMessage{
		DeviceID:   machine.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  machine.MachineID,
		BadgeID:    m.BadgeID,
		UsageState: "STARTED",
	})
}

// ---------------------------------------------------------------------------
// State: USING_MACHINE — user is actively exercising on a machine
// ---------------------------------------------------------------------------

func (e *Engine) stepUsingMachine(m *GymMember, now time.Time) {
	machine := e.findMachineByID(m.UsingMachineID)
	if machine == nil {
		// Machine vanished (shouldn't happen); recover.
		m.UsingMachineID = ""
		m.MachinesUsedHere++
		m.State = StateInArea
		m.PhaseStart = now
		return
	}

	if now.Before(machine.SessionEndTime) {
		return
	}

	// Session completed — stop the machine.
	e.publisher.PublishJSON("machine-usage", MachineUsageMessage{
		DeviceID:   machine.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  machine.MachineID,
		BadgeID:    m.BadgeID,
		UsageState: "STOPPED",
	})

	machine.Occupied = false
	machine.CurrentBadgeID = ""
	machine.SessionEndTime = time.Time{}
	m.UsingMachineID = ""
	m.MachinesUsedHere++

	// Go back to IN_AREA state to either pick another machine or leave.
	m.State = StateInArea
	m.PhaseStart = now
}

// ---------------------------------------------------------------------------
// State: LEAVING — user is back in "entrance-area", about to exit the gym
// ---------------------------------------------------------------------------

func (e *Engine) stepLeaving(m *GymMember, now time.Time) {
	if now.Before(m.NextTransition) {
		return
	}

	// Exit the entrance area.
	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  e.findAreaReader("entrance-area"),
		Timestamp: currentTimestamp(now),
		BadgeID:   m.BadgeID,
		AreaID:    "entrance-area",
		Direction: "OUT",
	})

	// Exit the gym.
	e.publisher.PublishJSON("gym-access", GymAccessMessage{
		DeviceID:   "turnstile-exit-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    m.BadgeID,
		AccessType: "EXIT",
	})

	// Reset to OUTSIDE.
	m.State = StateOutside
	m.CurrentAreaID = ""
	m.UsingMachineID = ""
	m.WorkoutPlan = nil
	m.CurrentPlanIndex = 0
	m.MachinesUsedHere = 0
	m.MachinesTarget = 0
	m.PhaseStart = time.Time{}
	m.NextTransition = now.Add(randomDurationBetween(e.rng, minOutsideWaitSeconds, maxOutsideWaitSeconds))
}

// ---------------------------------------------------------------------------
// Area transition helpers
// ---------------------------------------------------------------------------

// leaveCurrentArea handles leaving the current area and moving to the next
// one in the plan, or back to entrance if the plan is complete.
func (e *Engine) leaveCurrentArea(m *GymMember, now time.Time) {
	fromArea := m.CurrentAreaID

	if m.CurrentPlanIndex < len(m.WorkoutPlan) {
		// Move to the next area in the plan.
		nextAreaID := m.WorkoutPlan[m.CurrentPlanIndex]
		e.transitArea(m, fromArea, nextAreaID, now)
		m.CurrentPlanIndex++
		e.enterArea(m, nextAreaID, now)
	} else {
		// Plan is done — go back to entrance to leave.
		e.transitArea(m, fromArea, "entrance-area", now)
		e.transitionToLeaving(m, now)
	}
}

// transitArea publishes OUT from fromArea and IN to toArea.
func (e *Engine) transitArea(m *GymMember, fromArea, toArea string, now time.Time) {
	// Publish OUT from current area.
	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  e.findAreaReader(fromArea),
		Timestamp: currentTimestamp(now),
		BadgeID:   m.BadgeID,
		AreaID:    fromArea,
		Direction: "OUT",
	})

	// Publish IN to new area.
	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  e.findAreaReader(toArea),
		Timestamp: currentTimestamp(now),
		BadgeID:   m.BadgeID,
		AreaID:    toArea,
		Direction: "IN",
	})
}

// enterArea configures the member state for the area they just entered.
func (e *Engine) enterArea(m *GymMember, areaID string, now time.Time) {
	m.State = StateInArea
	m.CurrentAreaID = areaID
	m.PhaseStart = now
	m.MachinesUsedHere = 0
	m.UsingMachineID = ""

	area := e.findArea(areaID)
	if area != nil && area.HasMachines {
		m.MachinesTarget = randomIntBetween(e.rng, minMachinesPerArea, maxMachinesPerArea)
		m.NextTransition = time.Time{} // machines drive the timing
	} else {
		m.MachinesTarget = 0
		m.NextTransition = now.Add(randomDurationBetween(e.rng, minNoMachineStaySeconds, maxNoMachineStaySeconds))
	}
}

// transitionToLeaving moves the member to the LEAVING state in entrance.
func (e *Engine) transitionToLeaving(m *GymMember, now time.Time) {
	m.State = StateLeaving
	m.CurrentAreaID = "entrance-area"
	m.PhaseStart = now
	m.NextTransition = now.Add(randomDurationBetween(e.rng, minLeavingStaySeconds, maxLeavingStaySeconds))
	m.MachinesUsedHere = 0
	m.MachinesTarget = 0
	m.UsingMachineID = ""
}

// ---------------------------------------------------------------------------
// Device health
// ---------------------------------------------------------------------------

func (e *Engine) PublishDeviceStatuses(now time.Time) {
	e.mu.Lock()
	defer e.mu.Unlock()

	for i := range e.devices {
		d := &e.devices[i]
		e.updateDeviceHealth(d)

		status := "UP"
		if !d.Online {
			status = "DOWN"
		}

		e.publisher.PublishJSON("device-status", DeviceStatusMessage{
			DeviceID:     d.DeviceID,
			Timestamp:    currentTimestamp(now),
			DeviceType:   d.DeviceType,
			Online:       d.Online,
			StatusDetail: status,
		})
	}
}

func (e *Engine) updateDeviceHealth(device *Device) {
	if device.Online {
		if e.randomBool(device.FailRate) {
			device.Online = false
		}
		return
	}

	if e.randomBool(device.RecoverRate) {
		device.Online = true
	}
}

// ---------------------------------------------------------------------------
// Lookup helpers
// ---------------------------------------------------------------------------

func (e *Engine) findArea(areaID string) *Area {
	for i := range e.areas {
		if e.areas[i].AreaID == areaID {
			return &e.areas[i]
		}
	}
	return nil
}

func (e *Engine) findAreaReader(areaID string) string {
	for _, a := range e.areas {
		if a.AreaID == areaID {
			return a.ReaderID
		}
	}
	return ""
}

func (e *Engine) findMachineByID(machineID string) *Machine {
	for i := range e.machines {
		if e.machines[i].MachineID == machineID {
			return &e.machines[i]
		}
	}
	return nil
}

func (e *Engine) findFreeMachineInArea(areaID string) *Machine {
	candidates := make([]*Machine, 0)
	for i := range e.machines {
		if e.machines[i].AreaID == areaID && !e.machines[i].Occupied {
			candidates = append(candidates, &e.machines[i])
		}
	}
	if len(candidates) == 0 {
		return nil
	}
	return candidates[e.rng.Intn(len(candidates))]
}

// ---------------------------------------------------------------------------
// Workout plan builder
// ---------------------------------------------------------------------------

func (e *Engine) buildWorkoutPlan() []string {
	// Gather all area IDs except "entrance-area".
	areaIDs := make([]string, 0, len(e.areas))
	for _, a := range e.areas {
		if a.AreaID != "entrance-area" {
			areaIDs = append(areaIDs, a.AreaID)
		}
	}
	areaIDs = shuffleStrings(e.rng, uniqueStrings(areaIDs))

	if len(areaIDs) == 0 {
		return []string{}
	}

	planLength := randomIntBetween(e.rng, minPlanLength, min(maxPlanLength, len(areaIDs)))
	return areaIDs[:planLength]
}

// ---------------------------------------------------------------------------
// Random helpers
// ---------------------------------------------------------------------------

func (e *Engine) randomBool(p float64) bool {
	return e.rng.Float64() < p
}

func randomDurationBetween(rng RNG, minSec, maxSec int) time.Duration {
	if maxSec <= minSec {
		return time.Duration(minSec) * time.Second
	}
	n := rng.Intn(maxSec-minSec+1) + minSec
	return time.Duration(n) * time.Second
}

func randomIntBetween(rng RNG, minVal, maxVal int) int {
	if maxVal <= minVal {
		return minVal
	}
	return rng.Intn(maxVal-minVal+1) + minVal
}

func currentTimestamp(now time.Time) string {
	return now.UTC().Format(time.RFC3339)
}

func uniqueStrings(items []string) []string {
	seen := make(map[string]bool)
	result := make([]string, 0, len(items))
	for _, item := range items {
		if item == "" || seen[item] {
			continue
		}
		seen[item] = true
		result = append(result, item)
	}
	return result
}

func shuffleStrings(rng RNG, items []string) []string {
	cp := make([]string, len(items))
	copy(cp, items)
	rng.Shuffle(len(cp), func(i, j int) { cp[i], cp[j] = cp[j], cp[i] })
	return cp
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func badgeID(n int) string {
	return fmt.Sprintf("badge-%03d", n)
}

// ---------------------------------------------------------------------------
// Static data builders
// ---------------------------------------------------------------------------

func buildAreas() []Area {
	return []Area{
		{AreaID: "entrance-area", ReaderID: "reader-entrance-01", HasMachines: false},
		{AreaID: "cardio-area", ReaderID: "reader-cardio-01", HasMachines: true},
		{AreaID: "weight-area", ReaderID: "reader-weight-01", HasMachines: true},
		{AreaID: "machines-area", ReaderID: "reader-machines-01", HasMachines: true},
		{AreaID: "class-area", ReaderID: "reader-class-01", HasMachines: false},
	}
}

func buildMachines() []Machine {
	return []Machine{
		{MachineID: "treadmill-01", SensorID: "sensor-treadmill-01", AreaID: "cardio-area"},
		{MachineID: "treadmill-02", SensorID: "sensor-treadmill-02", AreaID: "cardio-area"},
		{MachineID: "treadmill-03", SensorID: "sensor-treadmill-03", AreaID: "cardio-area"},
		{MachineID: "bike-01", SensorID: "sensor-bike-01", AreaID: "cardio-area"},
		{MachineID: "bike-02", SensorID: "sensor-bike-02", AreaID: "cardio-area"},
		{MachineID: "bike-03", SensorID: "sensor-bike-03", AreaID: "cardio-area"},
		{MachineID: "rower-01", SensorID: "sensor-rower-01", AreaID: "cardio-area"},
		{MachineID: "rower-02", SensorID: "sensor-rower-02", AreaID: "cardio-area"},
		{MachineID: "elliptical-01", SensorID: "sensor-elliptical-01", AreaID: "cardio-area"},
		{MachineID: "elliptical-02", SensorID: "sensor-elliptical-02", AreaID: "cardio-area"},
		{MachineID: "benchpress-01", SensorID: "sensor-benchpress-01", AreaID: "weight-area"},
		{MachineID: "benchpress-02", SensorID: "sensor-benchpress-02", AreaID: "weight-area"},
		{MachineID: "legpress-01", SensorID: "sensor-legpress-01", AreaID: "weight-area"},
		{MachineID: "latpulldown-01", SensorID: "sensor-latpulldown-01", AreaID: "weight-area"},
		{MachineID: "shoulderpress-01", SensorID: "sensor-shoulderpress-01", AreaID: "weight-area"},
		{MachineID: "cablecross-01", SensorID: "sensor-cablecross-01", AreaID: "machines-area"},
		{MachineID: "cablecross-02", SensorID: "sensor-cablecross-02", AreaID: "machines-area"},
		{MachineID: "smithmachine-01", SensorID: "sensor-smithmachine-01", AreaID: "machines-area"},
		{MachineID: "smithmachine-02", SensorID: "sensor-smithmachine-02", AreaID: "machines-area"},
		{MachineID: "legextension-01", SensorID: "sensor-legextension-01", AreaID: "machines-area"},
		{MachineID: "legcurl-01", SensorID: "sensor-legcurl-01", AreaID: "machines-area"},
		{MachineID: "chestpress-01", SensorID: "sensor-chestpress-01", AreaID: "machines-area"},
		{MachineID: "chestpress-02", SensorID: "sensor-chestpress-02", AreaID: "machines-area"},
		{MachineID: "pecdeck-01", SensorID: "sensor-pecdeck-01", AreaID: "machines-area"},
		{MachineID: "hackmachine-01", SensorID: "sensor-hackmachine-01", AreaID: "machines-area"},
	}
}

func buildDevices(areas []Area, machines []Machine) []Device {
	devices := []Device{
		{DeviceID: "turnstile-entry-01", DeviceType: "TURNSTILE", Online: true, FailRate: 0.02, RecoverRate: 0.30},
		{DeviceID: "turnstile-exit-01", DeviceType: "TURNSTILE", Online: true, FailRate: 0.02, RecoverRate: 0.30},
	}
	for _, area := range areas {
		devices = append(devices, Device{DeviceID: area.ReaderID, DeviceType: "READER", Online: true, FailRate: 0.03, RecoverRate: 0.20})
	}
	for _, machine := range machines {
		devices = append(devices, Device{DeviceID: machine.SensorID, DeviceType: "SENSOR", Online: true, FailRate: 0.05, RecoverRate: 0.20})
	}
	return devices
}
