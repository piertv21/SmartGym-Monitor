package simulation

import (
	"sync"
	"time"

	"smartgym-monitor/simulator/internal/mqtt"
)

const (
	minSecondsBetweenActions = 8
	minGymStaySeconds        = 60
	minAreaStaySeconds       = 20

	minMachineSessionSeconds = 30
	maxMachineSessionSeconds = 120
)

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

type GymMember struct {
	BadgeID string

	InGym         bool
	GymEnteredAt  time.Time
	CurrentAreaID string
	AreaEnteredAt time.Time

	UsingMachineID string
	LastActionAt   time.Time

	WorkoutPlanAreas []string
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
	AreaID   string
	ReaderID string
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
		members = append(members, GymMember{BadgeID: badgeID(i)})
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

func (e *Engine) BusinessStep(now time.Time) {
	e.mu.Lock()
	defer e.mu.Unlock()

	for i := range e.members {
		m := &e.members[i]
		e.maybeStopMachine(m, now)
		e.maybeExitArea(m, now)
		e.maybeExitGym(m, now)
		e.maybeEnterGym(m, now)
		e.maybeMoveToArea(m, now)
		e.maybeStartMachine(m, now)
	}
}

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

func (e *Engine) maybeEnterGym(member *GymMember, now time.Time) {
	if member.InGym {
		return
	}
	if !canPerformAction(member, now) || !e.randomBool(0.15) {
		return
	}
	if !e.isDeviceOnline("turnstile-entry-01") {
		return
	}

	e.publisher.PublishJSON("gym-access", GymAccessMessage{
		DeviceID:   "turnstile-entry-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    member.BadgeID,
		AccessType: "ENTRY",
	})

	member.InGym = true
	member.GymEnteredAt = now
	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	member.UsingMachineID = ""
	member.WorkoutPlanAreas = e.buildWorkoutPlan()
	member.CurrentPlanIndex = 0
	markAction(member, now)
}

func (e *Engine) maybeMoveToArea(member *GymMember, now time.Time) {
	if !member.InGym || member.CurrentAreaID != "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) {
		return
	}

	var targetArea Area
	found := false
	if member.CurrentPlanIndex < len(member.WorkoutPlanAreas) {
		targetID := member.WorkoutPlanAreas[member.CurrentPlanIndex]
		for _, a := range e.areas {
			if a.AreaID == targetID {
				targetArea = a
				found = true
				break
			}
		}
	}
	if !found {
		targetArea = randomChoice(e.rng, e.areas)
	}

	if !e.isDeviceOnline(targetArea.ReaderID) || !e.randomBool(0.40) {
		return
	}

	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  targetArea.ReaderID,
		Timestamp: currentTimestamp(now),
		BadgeID:   member.BadgeID,
		AreaID:    targetArea.AreaID,
		Direction: "IN",
	})

	member.CurrentAreaID = targetArea.AreaID
	member.AreaEnteredAt = now
	markAction(member, now)
}

func (e *Engine) maybeStartMachine(member *GymMember, now time.Time) {
	if !member.InGym || member.CurrentAreaID == "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) {
		return
	}
	if now.Sub(member.AreaEnteredAt) < 10*time.Second || !e.randomBool(0.50) {
		return
	}

	m := e.findFreeMachineInArea(member.CurrentAreaID)
	if m == nil || !e.isDeviceOnline(m.SensorID) {
		return
	}

	m.Occupied = true
	m.CurrentBadgeID = member.BadgeID
	m.SessionEndTime = now.Add(randomDurationBetween(e.rng, minMachineSessionSeconds, maxMachineSessionSeconds))
	member.UsingMachineID = m.MachineID

	e.publisher.PublishJSON("machine-usage", MachineUsageMessage{
		DeviceID:   m.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  m.MachineID,
		BadgeID:    member.BadgeID,
		UsageState: "STARTED",
	})
	markAction(member, now)
}

func (e *Engine) maybeStopMachine(member *GymMember, now time.Time) {
	if member.UsingMachineID == "" {
		return
	}

	m := e.findMachineByID(member.UsingMachineID)
	if m == nil {
		member.UsingMachineID = ""
		return
	}
	if now.Before(m.SessionEndTime) || !e.isDeviceOnline(m.SensorID) {
		return
	}

	e.publisher.PublishJSON("machine-usage", MachineUsageMessage{
		DeviceID:   m.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  m.MachineID,
		BadgeID:    member.BadgeID,
		UsageState: "STOPPED",
	})

	m.Occupied = false
	m.CurrentBadgeID = ""
	m.SessionEndTime = time.Time{}
	member.UsingMachineID = ""
	markAction(member, now)
}

func (e *Engine) maybeExitArea(member *GymMember, now time.Time) {
	if !member.InGym || member.CurrentAreaID == "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) || now.Sub(member.AreaEnteredAt) < minAreaStaySeconds*time.Second {
		return
	}

	readerID := e.findAreaReader(member.CurrentAreaID)
	if readerID == "" || !e.isDeviceOnline(readerID) {
		return
	}

	shouldLeave := false
	if member.CurrentPlanIndex < len(member.WorkoutPlanAreas) && member.CurrentAreaID == member.WorkoutPlanAreas[member.CurrentPlanIndex] {
		shouldLeave = true
		member.CurrentPlanIndex++
	} else if e.randomBool(0.20) {
		shouldLeave = true
	}
	if !shouldLeave {
		return
	}

	e.publisher.PublishJSON("area-access", AreaAccessMessage{
		DeviceID:  readerID,
		Timestamp: currentTimestamp(now),
		BadgeID:   member.BadgeID,
		AreaID:    member.CurrentAreaID,
		Direction: "OUT",
	})

	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	markAction(member, now)
}

func (e *Engine) maybeExitGym(member *GymMember, now time.Time) {
	if !member.InGym || member.CurrentAreaID != "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) || now.Sub(member.GymEnteredAt) < minGymStaySeconds*time.Second {
		return
	}
	if !e.isDeviceOnline("turnstile-exit-01") {
		return
	}

	workoutCompleted := member.CurrentPlanIndex >= len(member.WorkoutPlanAreas)
	if !workoutCompleted && !e.randomBool(0.10) {
		return
	}

	e.publisher.PublishJSON("gym-access", GymAccessMessage{
		DeviceID:   "turnstile-exit-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    member.BadgeID,
		AccessType: "EXIT",
	})

	member.InGym = false
	member.GymEnteredAt = time.Time{}
	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	member.UsingMachineID = ""
	member.WorkoutPlanAreas = nil
	member.CurrentPlanIndex = 0
	markAction(member, now)
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

func (e *Engine) isDeviceOnline(id string) bool {
	for _, d := range e.devices {
		if d.DeviceID == id {
			return d.Online
		}
	}
	return false
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

func (e *Engine) buildWorkoutPlan() []string {
	areaIDs := make([]string, 0, len(e.areas))
	for _, a := range e.areas {
		areaIDs = append(areaIDs, a.AreaID)
	}
	areaIDs = shuffleStrings(e.rng, uniqueStrings(areaIDs))
	if len(areaIDs) == 0 {
		return []string{}
	}

	planLength := 1
	if len(areaIDs) >= 2 {
		planLength = e.rng.Intn(min(3, len(areaIDs))) + 1
	}
	return areaIDs[:planLength]
}

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

func currentTimestamp(now time.Time) string {
	return now.UTC().Format(time.RFC3339)
}

func randomChoice[T any](rng RNG, items []T) T {
	return items[rng.Intn(len(items))]
}

func canPerformAction(member *GymMember, now time.Time) bool {
	if member.LastActionAt.IsZero() {
		return true
	}
	return now.Sub(member.LastActionAt) >= minSecondsBetweenActions*time.Second
}

func markAction(member *GymMember, now time.Time) {
	member.LastActionAt = now
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
	if n < 10 {
		return "badge-00" + string(rune('0'+n))
	}
	if n < 100 {
		tens := n / 10
		ones := n % 10
		return "badge-0" + string(rune('0'+tens)) + string(rune('0'+ones))
	}
	return "badge-999"
}

func buildAreas() []Area {
	return []Area{
		{AreaID: "cardio-area", ReaderID: "reader-cardio-01"},
		{AreaID: "weight-area", ReaderID: "reader-weight-01"},
		{AreaID: "machines-area", ReaderID: "reader-machines-01"},
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
