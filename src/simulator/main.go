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
	mqttHost := getEnvOrDefault("MQTT_BROKER_HOST", "mosquitto")
	mqttPort := getEnvOrDefault("MQTT_BROKER_PORT", "1883")
	mqttProtocol := getEnvOrDefault("MQTT_BROKER_PROTOCOL", "tcp")
	mqttUsername := os.Getenv("MQTT_USERNAME")
	mqttPassword := os.Getenv("MQTT_PASSWORD")
	mqttTopic := getEnvOrDefault("MQTT_TOPIC", "smartgym/embedded/machine-sensor")

	opts := mqtt.NewClientOptions()
	brokerURL := fmt.Sprintf("%s://%s:%s", mqttProtocol, mqttHost, mqttPort)
	opts.AddBroker(brokerURL)
	opts.SetClientID("simulator")

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
