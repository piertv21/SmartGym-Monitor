package main

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

type MachineSensor struct {
	EventID   string  `json:"eventId"`
	MachineID string  `json:"machineId"`
	SensorID  string  `json:"sensorId"`
	Occupied  bool    `json:"occupied"`
	WeightKg  float64 `json:"weightKg"`
	Timestamp int64   `json:"timestamp"`
}

func getEnvOrDefault(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}

	return value
}

func main() {
	mqttHost := getEnvOrDefault("MQTT_BROKER_HOST", "e30656ac89584a55aaed5759c47e363e.s1.eu.hivemq.cloud")
	mqttPort := getEnvOrDefault("MQTT_BROKER_PORT", "8883")
	mqttUsername := getEnvOrDefault("MQTT_USERNAME", "SmartGym-Monitor")
	mqttPassword := getEnvOrDefault("MQTT_PASSWORD", "SmartGym-Monitor1")
	mqttTopic := getEnvOrDefault("MQTT_TOPIC", "smartgym/embedded/machine-sensor")

	opts := mqtt.NewClientOptions()
	opts.AddBroker(fmt.Sprintf("tls://%s:%s", mqttHost, mqttPort))
	opts.SetClientID("simulator")
	opts.SetUsername(mqttUsername)
	opts.SetPassword(mqttPassword)

	opts.SetTLSConfig(&tls.Config{
		MinVersion: tls.VersionTLS12,
	})

	client := mqtt.NewClient(opts)

	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	fmt.Println("Connected to HiveMQ")

	for {
		msg := MachineSensor{
			EventID:   fmt.Sprintf("evt-%d", time.Now().UnixMilli()),
			MachineID: "bench-press-01",
			SensorID:  "sensor-01",
			Occupied:  true,
			WeightKg:  80.0,
			Timestamp: time.Now().UnixMilli(),
		}

		fmt.Printf("Message: %+v\n", msg)

		data, err := json.Marshal(msg)
		if err != nil {
			log.Println("marshal error:", err)
			continue
		}

		token := client.Publish(mqttTopic, 1, false, data)
		token.Wait()

		if token.Error() != nil {
			log.Println("publish error:", token.Error())
		} else {
			fmt.Println("Message sent:", msg.EventID)
		}

		time.Sleep(5 * time.Second)
	}
}
