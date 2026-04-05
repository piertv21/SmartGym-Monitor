package mqtt

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log"
	"time"

	paho "github.com/eclipse/paho.mqtt.golang"

	"smartgym-monitor/simulator/internal/config"
)

type Publisher interface {
	PublishJSON(topicSuffix string, payload any)
}

type JSONPublisher struct {
	client    paho.Client
	baseTopic string
}

func NewClient(cfg config.Config) (paho.Client, string, error) {
	opts := paho.NewClientOptions()
	brokerURL := fmt.Sprintf("%s://%s:%s", cfg.MQTTProtocol, cfg.MQTTHost, cfg.MQTTPort)
	opts.AddBroker(brokerURL)
	opts.SetClientID(fmt.Sprintf("simulator-%d", time.Now().UnixNano()))
	opts.SetAutoReconnect(true)
	opts.SetConnectRetry(true)
	opts.SetConnectRetryInterval(2 * time.Second)

	if cfg.MQTTUsername != "" {
		opts.SetUsername(cfg.MQTTUsername)
	}
	if cfg.MQTTPassword != "" {
		opts.SetPassword(cfg.MQTTPassword)
	}
	if cfg.MQTTProtocol == "tls" || cfg.MQTTProtocol == "ssl" {
		opts.SetTLSConfig(&tls.Config{MinVersion: tls.VersionTLS12})
	}

	client := paho.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		return nil, brokerURL, token.Error()
	}

	return client, brokerURL, nil
}

func NewJSONPublisher(client paho.Client, baseTopic string) *JSONPublisher {
	return &JSONPublisher{client: client, baseTopic: baseTopic}
}

func (p *JSONPublisher) PublishJSON(topicSuffix string, payload any) {
	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("marshal error on topic %s/%s: %v", p.baseTopic, topicSuffix, err)
		return
	}

	topic := p.baseTopic + "/" + topicSuffix
	token := p.client.Publish(topic, 1, false, data)
	token.Wait()
	if token.Error() != nil {
		log.Printf("publish error on topic %s: %v", topic, token.Error())
		return
	}

	fmt.Printf("Publishing %s -> %s\n", topic, string(data))
}
