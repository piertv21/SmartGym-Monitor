package simulation

import (
	"math/rand"
	"testing"
	"time"
)

type fakePublisher struct {
	count int
}

func (f *fakePublisher) PublishJSON(_ string, _ any) {
	f.count++
}

func TestEngineSmoke(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(42))
	engine := NewEngine(pub, rng)


	areas, machines, devices, members := engine.Counts()
	if areas == 0 || machines == 0 || devices == 0 || members == 0 {
		t.Fatalf("unexpected empty state: %d %d %d %d", areas, machines, devices, members)
	}

	now := time.Now()
	for i := 0; i < 50; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}
	engine.PublishDeviceStatuses(now)

	if pub.count == 0 {
		t.Fatal("expected at least one published message")
	}
}
