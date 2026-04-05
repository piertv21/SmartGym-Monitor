package config

import "testing"

func TestLoadDefaults(t *testing.T) {
	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	if cfg.MQTTHost != "mosquitto" {
		t.Fatalf("unexpected MQTTHost: %s", cfg.MQTTHost)
	}
	if cfg.BaseTopic != "smartgym" {
		t.Fatalf("unexpected BaseTopic: %s", cfg.BaseTopic)
	}
}

func TestLoadInvalidDuration(t *testing.T) {
	t.Setenv("SIM_BUSINESS_TICK", "oops")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error for invalid duration")
	}
}
