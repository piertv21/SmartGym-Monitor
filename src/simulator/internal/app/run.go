package app

import (
	"context"
	"fmt"
	"log"
	"math/rand"
	"runtime/debug"
	"sync"
	"time"

	"smartgym-monitor/simulator/internal/config"
	"smartgym-monitor/simulator/internal/mqtt"
	"smartgym-monitor/simulator/internal/simulation"
)

func Run(ctx context.Context) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}

	seed := cfg.Seed
	if seed == 0 {
		seed = time.Now().UnixNano()
	}
	rng := rand.New(rand.NewSource(seed))

	client, brokerURL, err := mqtt.NewClient(cfg)
	if err != nil {
		return err
	}
	defer client.Disconnect(250)

	publisher := mqtt.NewJSONPublisher(client, cfg.BaseTopic)
	engine := simulation.NewEngine(publisher, rng)
	areas, machines, devices, members := engine.Counts()

	log.Printf("Connected to MQTT broker: %s", brokerURL)
	log.Printf("Base topic: %s", cfg.BaseTopic)
	log.Printf("Seed: %d", seed)
	log.Printf("Loaded %d machines across %d areas (%d devices, %d members)", machines, areas, devices, members)

	wg := sync.WaitGroup{}
	wg.Add(2)

	go func() {
		defer wg.Done()
		runSupervisedWorker(ctx, "business-loop", func(ctx context.Context) {
			ticker := time.NewTicker(cfg.BusinessTick)
			defer ticker.Stop()
			for {
				select {
				case <-ctx.Done():
					return
				case now := <-ticker.C:
					engine.BusinessStep(now)
				}
			}
		})
	}()

	go func() {
		defer wg.Done()
		runSupervisedWorker(ctx, "health-loop", func(ctx context.Context) {
			ticker := time.NewTicker(cfg.HealthTick)
			defer ticker.Stop()
			for {
				select {
				case <-ctx.Done():
					return
				case now := <-ticker.C:
					engine.PublishDeviceStatuses(now)
				}
			}
		})
	}()

	<-ctx.Done()
	wg.Wait()
	return nil
}

func runSupervisedWorker(ctx context.Context, name string, worker func(context.Context)) {
	for {
		if ctx.Err() != nil {
			return
		}

		err := runOnce(ctx, worker)
		if err == nil || ctx.Err() != nil {
			return
		}

		log.Printf("worker %s crashed: %v; restarting", name, err)
		select {
		case <-ctx.Done():
			return
		case <-time.After(1500 * time.Millisecond):
		}
	}
}

func runOnce(ctx context.Context, worker func(context.Context)) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("panic: %v\n%s", r, string(debug.Stack()))
		}
	}()
	worker(ctx)
	return nil
}
