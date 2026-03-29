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
// ==================== MESSAGES ====================
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
// ==================== STATE ====================
//

type GymMember struct {
	BadgeID        string
	InGym          bool
	CurrentAreaID  string
	UsingMachineID string
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

func publishJSON(client mqtt.Client, topic string, payload any) {
	data, _ := json.Marshal(payload)

	token := client.Publish(topic, 1, false, data)
	token.Wait()

	fmt.Printf("Publishing %s -> %s\n", topic, string(data))
}

func isDeviceOnline(devices []Device, id string) bool {
	for _, d := range devices {
		if d.DeviceID == id {
			return d.Online
		}
	}
	return false
}

func randomChoice[T any](items []T) T {
	return items[rand.Intn(len(items))]
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
	now := time.Now().UTC().Format(time.RFC3339)

	for i := range devices {
		d := &devices[i]
		updateDeviceHealth(d)

		status := "UP"
		if !d.Online {
			status = "DOWN"
		}

		msg := DeviceStatusMessage{
			DeviceID:     d.DeviceID,
			Timestamp:    now,
			DeviceType:   d.DeviceType,
			Online:       d.Online,
			StatusDetail: status,
		}

		publishJSON(client, baseTopic+"/device-status", msg)
	}
}

//
// ==================== SIMULATION ====================
//

func maybeEnterGym(client mqtt.Client, baseTopic string, member *GymMember, devices []Device) {
	if member.InGym || !randomBool(0.15) {
		return
	}

	if !isDeviceOnline(devices, "turnstile-entry-01") {
		return
	}

	msg := GymAccessMessage{
		DeviceID:   "turnstile-entry-01",
		Timestamp:  time.Now().UTC().Format(time.RFC3339),
		BadgeID:    member.BadgeID,
		AccessType: "ENTRY",
	}

	publishJSON(client, baseTopic+"/gym-access", msg)
	member.InGym = true
}

func maybeExitArea(client mqtt.Client, baseTopic string, member *GymMember, areas []Area, devices []Device) {
	if member.CurrentAreaID == "" {
		return
	}

	// Find the area and its reader
	var areaReader string
	for _, a := range areas {
		if a.AreaID == member.CurrentAreaID {
			areaReader = a.ReaderID
			break
		}
	}

	if areaReader == "" || !isDeviceOnline(devices, areaReader) {
		return
	}

	msg := AreaAccessMessage{
		DeviceID:  areaReader,
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		BadgeID:   member.BadgeID,
		AreaID:    member.CurrentAreaID,
		Direction: "OUT",
	}

	publishJSON(client, baseTopic+"/area-access", msg)
	member.CurrentAreaID = ""
}

func maybeMoveToArea(client mqtt.Client, baseTopic string, member *GymMember, areas []Area, devices []Device) {
	if !member.InGym || member.UsingMachineID != "" {
		return
	}

	if member.CurrentAreaID == "" && randomBool(0.2) {
		area := randomChoice(areas)

		if !isDeviceOnline(devices, area.ReaderID) {
			return
		}

		msg := AreaAccessMessage{
			DeviceID:  area.ReaderID,
			Timestamp: time.Now().UTC().Format(time.RFC3339),
			BadgeID:   member.BadgeID,
			AreaID:    area.AreaID,
			Direction: "IN",
		}

		publishJSON(client, baseTopic+"/area-access", msg)
		member.CurrentAreaID = area.AreaID
	}
}

func maybeStartMachine(client mqtt.Client, baseTopic string, member *GymMember, machines []Machine, devices []Device) {
	if member.CurrentAreaID == "" || member.UsingMachineID != "" || !randomBool(0.25) {
		return
	}

	for i := range machines {
		m := &machines[i]

		if m.AreaID == member.CurrentAreaID && !m.Occupied {
			if !isDeviceOnline(devices, m.SensorID) {
				return
			}

			now := time.Now().UTC()

			m.Occupied = true
			m.CurrentBadgeID = member.BadgeID
			m.SessionEndTime = now.Add(30 * time.Second)
			member.UsingMachineID = m.MachineID

			msg := MachineUsageMessage{
				DeviceID:   m.SensorID,
				Timestamp:  now.Format(time.RFC3339),
				MachineID:  m.MachineID,
				BadgeID:    member.BadgeID,
				UsageState: "STARTED",
			}

			publishJSON(client, baseTopic+"/machine-usage", msg)
			return
		}
	}
}

func maybeStopMachine(client mqtt.Client, baseTopic string, member *GymMember, machines []Machine, devices []Device) {
	if member.UsingMachineID == "" {
		return
	}

	for i := range machines {
		m := &machines[i]

		if m.MachineID == member.UsingMachineID && time.Now().After(m.SessionEndTime) {
			if !isDeviceOnline(devices, m.SensorID) {
				return
			}

			msg := MachineUsageMessage{
				DeviceID:   m.SensorID,
				Timestamp:  time.Now().UTC().Format(time.RFC3339),
				MachineID:  m.MachineID,
				BadgeID:    member.BadgeID,
				UsageState: "STOPPED",
			}

			publishJSON(client, baseTopic+"/machine-usage", msg)

			m.Occupied = false
			m.CurrentBadgeID = ""
			member.UsingMachineID = ""
			return
		}
	}
}

func maybeExitGym(client mqtt.Client, baseTopic string, member *GymMember, devices []Device) {
	if !member.InGym || member.UsingMachineID != "" || !randomBool(0.08) {
		return
	}

	if !isDeviceOnline(devices, "turnstile-exit-01") {
		return
	}

	msg := GymAccessMessage{
		DeviceID:   "turnstile-exit-01",
		Timestamp:  time.Now().UTC().Format(time.RFC3339),
		BadgeID:    member.BadgeID,
		AccessType: "EXIT",
	}

	publishJSON(client, baseTopic+"/gym-access", msg)

	member.InGym = false
	member.CurrentAreaID = ""
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

	areas := []Area{
		{"cardio-area", "reader-cardio-01"},
		{"weight-area", "reader-weight-01"},
	}

	machines := []Machine{
		{"treadmill-01", "sensor-treadmill-01", "cardio-area", false, "", time.Time{}},
		{"bench-press-01", "sensor-bench-01", "weight-area", false, "", time.Time{}},
	}

	members := []GymMember{
		{"badge-001", false, "", ""},
		{"badge-002", false, "", ""},
	}

	devices := []Device{
		{"turnstile-entry-01", "TURNSTILE", true, 0.02, 0.3},
		{"turnstile-exit-01", "TURNSTILE", true, 0.02, 0.3},
		{"reader-cardio-01", "READER", true, 0.03, 0.2},
		{"reader-weight-01", "READER", true, 0.03, 0.2},
		{"sensor-treadmill-01", "SENSOR", true, 0.05, 0.2},
		{"sensor-bench-01", "SENSOR", true, 0.05, 0.2},
	}

	businessTicker := time.NewTicker(4 * time.Second)
	healthTicker := time.NewTicker(30 * time.Second)

	for {
		select {
		case <-businessTicker.C:
			for i := range members {
				m := &members[i]

				maybeEnterGym(client, baseTopic, m, devices)
				maybeMoveToArea(client, baseTopic, m, areas, devices)
				maybeStartMachine(client, baseTopic, m, machines, devices)
				maybeStopMachine(client, baseTopic, m, machines, devices)
				maybeExitArea(client, baseTopic, m, areas, devices)
				maybeExitGym(client, baseTopic, m, devices)
			}

		case <-healthTicker.C:
			publishDeviceStatuses(client, baseTopic, devices)
		}
	}
}