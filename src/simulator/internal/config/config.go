package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	MQTTHost     string
	MQTTPort     string
	MQTTProtocol string
	MQTTUsername string
	MQTTPassword string
	BaseTopic    string

	BusinessTick time.Duration
	HealthTick   time.Duration
	Seed         int64
}

func Load() (Config, error) {
	cfg := Config{
		MQTTHost:     getEnvOrDefault("MQTT_BROKER_HOST", "mosquitto"),
		MQTTPort:     getEnvOrDefault("MQTT_BROKER_PORT", "1883"),
		MQTTProtocol: getEnvOrDefault("MQTT_BROKER_PROTOCOL", "tcp"),
		MQTTUsername: os.Getenv("MQTT_USERNAME"),
		MQTTPassword: os.Getenv("MQTT_PASSWORD"),
		BaseTopic:    getEnvOrDefault("MQTT_TOPIC", "smartgym"),
		BusinessTick: 4 * time.Second,
		HealthTick:   30 * time.Second,
	}

	if v := os.Getenv("SIM_BUSINESS_TICK"); v != "" {
		d, err := time.ParseDuration(v)
		if err != nil {
			return Config{}, fmt.Errorf("invalid SIM_BUSINESS_TICK: %w", err)
		}
		cfg.BusinessTick = d
	}

	if v := os.Getenv("SIM_HEALTH_TICK"); v != "" {
		d, err := time.ParseDuration(v)
		if err != nil {
			return Config{}, fmt.Errorf("invalid SIM_HEALTH_TICK: %w", err)
		}
		cfg.HealthTick = d
	}

	if v := os.Getenv("SIM_SEED"); v != "" {
		seed, err := strconv.ParseInt(v, 10, 64)
		if err != nil {
			return Config{}, fmt.Errorf("invalid SIM_SEED: %w", err)
		}
		cfg.Seed = seed
	}

	if cfg.BusinessTick <= 0 {
		return Config{}, fmt.Errorf("SIM_BUSINESS_TICK must be > 0")
	}
	if cfg.HealthTick <= 0 {
		return Config{}, fmt.Errorf("SIM_HEALTH_TICK must be > 0")
	}

	return cfg, nil
}

func getEnvOrDefault(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
