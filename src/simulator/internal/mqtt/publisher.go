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
	msgQueue  chan msgItem
}

type msgItem struct {
	topic string
	data  []byte
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
	p := &JSONPublisher{
		client:    client,
		baseTopic: baseTopic,
		msgQueue:  make(chan msgItem, 10000),
	}
	go p.worker()
	return p
}

func (p *JSONPublisher) worker() {
	for msg := range p.msgQueue {
		token := p.client.Publish(msg.topic, 1, false, msg.data)
		token.Wait()
		if token.Error() != nil {
			log.Printf("publish error on topic %s: %v", msg.topic, token.Error())
			continue
		}

		fmt.Printf("Publishing %s -> %s\n", msg.topic, string(msg.data))
		time.Sleep(50 * time.Millisecond)
	}
}

func (p *JSONPublisher) PublishJSON(topicSuffix string, payload any) {
	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("marshal error on topic %s/%s: %v", p.baseTopic, topicSuffix, err)
		return
	}

	topic := p.baseTopic + "/" + topicSuffix

	select {
	case p.msgQueue <- msgItem{topic: topic, data: data}:
	default:
		log.Printf("warning: mqtt publish queue is full, dropping message %s", topic)
	}
}
