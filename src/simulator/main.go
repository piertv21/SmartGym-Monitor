package main

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

//
// ==================== MQTT MESSAGES ====================
//

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

//
// ==================== DOMAIN ====================
//

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

//
// ==================== CONFIG ====================
//

const (
	businessTick = 4 * time.Second

	minSecondsBetweenActions = 8
	minGymStaySeconds        = 60
	minAreaStaySeconds       = 20

	minMachineSessionSeconds = 30
	maxMachineSessionSeconds = 120
)

//
// ==================== UTILS ====================
//

func getEnvOrDefault(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}

func randomBool(p float64) bool {
	return rand.Float64() < p
}

func randomDurationBetween(minSec, maxSec int) time.Duration {
	if maxSec <= minSec {
		return time.Duration(minSec) * time.Second
	}
	n := rand.Intn(maxSec-minSec+1) + minSec
	return time.Duration(n) * time.Second
}

func publishJSON(client mqtt.Client, topic string, payload any) {
	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("marshal error on topic %s: %v", topic, err)
		return
	}

	token := client.Publish(topic, 1, false, data)
	token.Wait()
	if token.Error() != nil {
		log.Printf("publish error on topic %s: %v", topic, token.Error())
		return
	}

	fmt.Printf("Publishing %s -> %s\n", topic, string(data))
}

func currentTimestamp(now time.Time) string {
	return now.UTC().Format(time.RFC3339)
}

func randomChoice[T any](items []T) T {
	return items[rand.Intn(len(items))]
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

func shuffleStrings(items []string) []string {
	cp := make([]string, len(items))
	copy(cp, items)
	rand.Shuffle(len(cp), func(i, j int) {
		cp[i], cp[j] = cp[j], cp[i]
	})
	return cp
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

//
// ==================== HARDCODED DATA ====================
//

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
		devices = append(devices, Device{
			DeviceID:    area.ReaderID,
			DeviceType:  "READER",
			Online:      true,
			FailRate:    0.03,
			RecoverRate: 0.20,
		})
	}

	for _, machine := range machines {
		devices = append(devices, Device{
			DeviceID:    machine.SensorID,
			DeviceType:  "SENSOR",
			Online:      true,
			FailRate:    0.05,
			RecoverRate: 0.20,
		})
	}

	return devices
}

//
// ==================== LOOKUPS ====================
//

func isDeviceOnline(devices []Device, id string) bool {
	for _, d := range devices {
		if d.DeviceID == id {
			return d.Online
		}
	}
	return false
}

func findAreaReader(areas []Area, areaID string) string {
	for _, a := range areas {
		if a.AreaID == areaID {
			return a.ReaderID
		}
	}
	return ""
}

func findMachineByID(machines []Machine, machineID string) *Machine {
	for i := range machines {
		if machines[i].MachineID == machineID {
			return &machines[i]
		}
	}
	return nil
}

func findFreeMachineInArea(machines []Machine, areaID string) *Machine {
	var candidates []*Machine
	for i := range machines {
		if machines[i].AreaID == areaID && !machines[i].Occupied {
			candidates = append(candidates, &machines[i])
		}
	}
	if len(candidates) == 0 {
		return nil
	}
	return candidates[rand.Intn(len(candidates))]
}

func buildWorkoutPlan(areas []Area) []string {
	areaIDs := make([]string, 0, len(areas))
	for _, a := range areas {
		areaIDs = append(areaIDs, a.AreaID)
	}

	areaIDs = shuffleStrings(uniqueStrings(areaIDs))
	if len(areaIDs) == 0 {
		return []string{}
	}

	planLength := 1
	if len(areaIDs) >= 2 {
		planLength = rand.Intn(min(3, len(areaIDs))) + 1
	}

	return areaIDs[:planLength]
}

//
// ==================== DEVICE HEALTH ====================
//

func updateDeviceHealth(device *Device) {
	if device.Online {
		if randomBool(device.FailRate) {
			device.Online = false
		}
	} else {
		if randomBool(device.RecoverRate) {
			device.Online = true
		}
	}
}

func publishDeviceStatuses(client mqtt.Client, baseTopic string, devices []Device) {
	now := time.Now().UTC()

	for i := range devices {
		d := &devices[i]
		updateDeviceHealth(d)

		status := "UP"
		if !d.Online {
			status = "DOWN"
		}

		msg := DeviceStatusMessage{
			DeviceID:     d.DeviceID,
			Timestamp:    currentTimestamp(now),
			DeviceType:   d.DeviceType,
			Online:       d.Online,
			StatusDetail: status,
		}

		publishJSON(client, baseTopic+"/device-status", msg)
	}
}

//
// ==================== BUSINESS LOGIC ====================
//

func maybeEnterGym(client mqtt.Client, baseTopic string, member *GymMember, devices []Device, areas []Area, now time.Time) {
	if member.InGym {
		return
	}
	if !canPerformAction(member, now) || !randomBool(0.15) {
		return
	}
	if !isDeviceOnline(devices, "turnstile-entry-01") {
		return
	}

	msg := GymAccessMessage{
		DeviceID:   "turnstile-entry-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    member.BadgeID,
		AccessType: "ENTRY",
	}
	publishJSON(client, baseTopic+"/gym-access", msg)

	member.InGym = true
	member.GymEnteredAt = now
	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	member.UsingMachineID = ""
	member.WorkoutPlanAreas = buildWorkoutPlan(areas)
	member.CurrentPlanIndex = 0
	markAction(member, now)
}

func maybeMoveToArea(client mqtt.Client, baseTopic string, member *GymMember, areas []Area, devices []Device, now time.Time) {
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
		for _, a := range areas {
			if a.AreaID == targetID {
				targetArea = a
				found = true
				break
			}
		}
	}

	if !found {
		targetArea = randomChoice(areas)
	}

	if !isDeviceOnline(devices, targetArea.ReaderID) {
		return
	}
	if !randomBool(0.40) {
		return
	}

	msg := AreaAccessMessage{
		DeviceID:  targetArea.ReaderID,
		Timestamp: currentTimestamp(now),
		BadgeID:   member.BadgeID,
		AreaID:    targetArea.AreaID,
		Direction: "IN",
	}
	publishJSON(client, baseTopic+"/area-access", msg)

	member.CurrentAreaID = targetArea.AreaID
	member.AreaEnteredAt = now
	markAction(member, now)
}

func maybeStartMachine(client mqtt.Client, baseTopic string, member *GymMember, machines []Machine, devices []Device, now time.Time) {
	if !member.InGym || member.CurrentAreaID == "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) {
		return
	}
	if now.Sub(member.AreaEnteredAt) < 10*time.Second {
		return
	}
	if !randomBool(0.50) {
		return
	}

	m := findFreeMachineInArea(machines, member.CurrentAreaID)
	if m == nil {
		return
	}
	if !isDeviceOnline(devices, m.SensorID) {
		return
	}

	sessionDuration := randomDurationBetween(minMachineSessionSeconds, maxMachineSessionSeconds)

	m.Occupied = true
	m.CurrentBadgeID = member.BadgeID
	m.SessionEndTime = now.Add(sessionDuration)

	member.UsingMachineID = m.MachineID

	msg := MachineUsageMessage{
		DeviceID:   m.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  m.MachineID,
		BadgeID:    member.BadgeID,
		UsageState: "STARTED",
	}
	publishJSON(client, baseTopic+"/machine-usage", msg)
	markAction(member, now)
}

func maybeStopMachine(client mqtt.Client, baseTopic string, member *GymMember, machines []Machine, devices []Device, now time.Time) {
	if member.UsingMachineID == "" {
		return
	}

	m := findMachineByID(machines, member.UsingMachineID)
	if m == nil {
		member.UsingMachineID = ""
		return
	}
	if now.Before(m.SessionEndTime) {
		return
	}
	if !isDeviceOnline(devices, m.SensorID) {
		return
	}

	msg := MachineUsageMessage{
		DeviceID:   m.SensorID,
		Timestamp:  currentTimestamp(now),
		MachineID:  m.MachineID,
		BadgeID:    member.BadgeID,
		UsageState: "STOPPED",
	}
	publishJSON(client, baseTopic+"/machine-usage", msg)

	m.Occupied = false
	m.CurrentBadgeID = ""
	m.SessionEndTime = time.Time{}
	member.UsingMachineID = ""
	markAction(member, now)
}

func maybeExitArea(client mqtt.Client, baseTopic string, member *GymMember, areas []Area, devices []Device, now time.Time) {
	if !member.InGym || member.CurrentAreaID == "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) {
		return
	}
	if now.Sub(member.AreaEnteredAt) < minAreaStaySeconds*time.Second {
		return
	}

	readerID := findAreaReader(areas, member.CurrentAreaID)
	if readerID == "" || !isDeviceOnline(devices, readerID) {
		return
	}

	shouldLeave := false

	if member.CurrentPlanIndex < len(member.WorkoutPlanAreas) && member.CurrentAreaID == member.WorkoutPlanAreas[member.CurrentPlanIndex] {
		shouldLeave = true
		member.CurrentPlanIndex++
	} else if randomBool(0.20) {
		shouldLeave = true
	}

	if !shouldLeave {
		return
	}

	msg := AreaAccessMessage{
		DeviceID:  readerID,
		Timestamp: currentTimestamp(now),
		BadgeID:   member.BadgeID,
		AreaID:    member.CurrentAreaID,
		Direction: "OUT",
	}
	publishJSON(client, baseTopic+"/area-access", msg)

	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	markAction(member, now)
}

func maybeExitGym(client mqtt.Client, baseTopic string, member *GymMember, devices []Device, now time.Time) {
	if !member.InGym || member.CurrentAreaID != "" || member.UsingMachineID != "" {
		return
	}
	if !canPerformAction(member, now) {
		return
	}
	if now.Sub(member.GymEnteredAt) < minGymStaySeconds*time.Second {
		return
	}
	if !isDeviceOnline(devices, "turnstile-exit-01") {
		return
	}

	workoutCompleted := member.CurrentPlanIndex >= len(member.WorkoutPlanAreas)
	if !workoutCompleted && !randomBool(0.10) {
		return
	}

	msg := GymAccessMessage{
		DeviceID:   "turnstile-exit-01",
		Timestamp:  currentTimestamp(now),
		BadgeID:    member.BadgeID,
		AccessType: "EXIT",
	}
	publishJSON(client, baseTopic+"/gym-access", msg)

	member.InGym = false
	member.GymEnteredAt = time.Time{}
	member.CurrentAreaID = ""
	member.AreaEnteredAt = time.Time{}
	member.UsingMachineID = ""
	member.WorkoutPlanAreas = nil
	member.CurrentPlanIndex = 0
	markAction(member, now)
}

//
// ==================== MAIN ====================
//

func main() {
	rand.Seed(time.Now().UnixNano())

	mqttHost := getEnvOrDefault("MQTT_BROKER_HOST", "mosquitto")
	mqttPort := getEnvOrDefault("MQTT_BROKER_PORT", "1883")
	mqttProtocol := getEnvOrDefault("MQTT_BROKER_PROTOCOL", "tcp")
	mqttUsername := os.Getenv("MQTT_USERNAME")
	mqttPassword := os.Getenv("MQTT_PASSWORD")
	baseTopic := getEnvOrDefault("MQTT_TOPIC", "smartgym")

	areas := buildAreas()
	machines := buildMachines()
	devices := buildDevices(areas, machines)

	log.Printf("Loaded %d hardcoded machines across %d areas", len(machines), len(areas))

	opts := mqtt.NewClientOptions()
	brokerURL := fmt.Sprintf("%s://%s:%s", mqttProtocol, mqttHost, mqttPort)
	opts.AddBroker(brokerURL)
	opts.SetClientID(fmt.Sprintf("simulator-%d", time.Now().UnixNano()))

	if mqttUsername != "" {
		opts.SetUsername(mqttUsername)
	}
	if mqttPassword != "" {
		opts.SetPassword(mqttPassword)
	}
	if mqttProtocol == "tls" || mqttProtocol == "ssl" {
		opts.SetTLSConfig(&tls.Config{
			MinVersion: tls.VersionTLS12,
		})
	}

	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	fmt.Println("Connected to MQTT broker:", brokerURL)
	fmt.Println("Base topic:", baseTopic)

	members := []GymMember{
		{BadgeID: "badge-001"},
		//{BadgeID: "badge-002"},
		//{BadgeID: "badge-003"},
        //{BadgeID: "badge-004"},
	}

	businessTicker := time.NewTicker(businessTick)
	// healthTicker := time.NewTicker(30 * time.Second)
	defer businessTicker.Stop()

	for {
		select {
		case <-businessTicker.C:
			now := time.Now()

			for i := range members {
				m := &members[i]

				maybeStopMachine(client, baseTopic, m, machines, devices, now)
				maybeExitArea(client, baseTopic, m, areas, devices, now)
				maybeExitGym(client, baseTopic, m, devices, now)

				maybeEnterGym(client, baseTopic, m, devices, areas, now)
				maybeMoveToArea(client, baseTopic, m, areas, devices, now)
				maybeStartMachine(client, baseTopic, m, machines, devices, now)
			}

		// case <-healthTicker.C:
		// 	publishDeviceStatuses(client, baseTopic, devices)
		}
	}
}
