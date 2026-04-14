package simulation

import (
	"math/rand"
	"testing"
	"time"
)

type fakePublisher struct {
	count    int
	messages []publishedMsg
}

type publishedMsg struct {
	topic   string
	payload any
}

func (f *fakePublisher) PublishJSON(topic string, payload any) {
	f.count++
	f.messages = append(f.messages, publishedMsg{topic: topic, payload: payload})
}

func TestEngineSmoke(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(42))
	engine := NewEngine(pub, rng)

	areas, machines, devices, members := engine.Counts()
	if areas != 5 {
		t.Fatalf("expected 5 areas, got %d", areas)
	}
	if machines != 25 {
		t.Fatalf("expected 25 machines, got %d", machines)
	}
	if devices == 0 {
		t.Fatalf("expected devices > 0, got %d", devices)
	}
	if members != 15 {
		t.Fatalf("expected 15 members, got %d", members)
	}

	// Run enough ticks to see some activity.
	now := time.Now()
	for i := 0; i < 200; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}
	engine.PublishDeviceStatuses(now)

	if pub.count == 0 {
		t.Fatal("expected at least one published message")
	}
}

func TestMemberCompletesFullCycle(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(123))
	engine := NewEngine(pub, rng)

	// Advance the simulation far enough that at least one member completes
	// a full cycle: OUTSIDE → IN_LOBBY → IN_AREA → ... → LEAVING → OUTSIDE.
	now := time.Now()
	totalTicks := 500
	for i := 0; i < totalTicks; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}

	// Check that we saw both ENTRY and EXIT gym-access messages.
	entries := 0
	exits := 0
	for _, msg := range pub.messages {
		if msg.topic == "gym-access" {
			if gam, ok := msg.payload.(GymAccessMessage); ok {
				switch gam.AccessType {
				case "ENTRY":
					entries++
				case "EXIT":
					exits++
				}
			}
		}
	}

	if entries == 0 {
		t.Fatal("expected at least one gym ENTRY message")
	}
	if exits == 0 {
		t.Fatal("expected at least one gym EXIT message")
	}
}

func TestAllMembersStartOutside(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(99))
	engine := NewEngine(pub, rng)

	// Before any step, all members should be OUTSIDE.
	engine.mu.Lock()
	for _, m := range engine.members {
		if m.State != StateOutside {
			t.Fatalf("member %s should be OUTSIDE, got %s", m.BadgeID, m.State)
		}
	}
	engine.mu.Unlock()
}

func TestIngressoIsAlwaysTransit(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(77))
	engine := NewEngine(pub, rng)

	// Run simulation and verify that no machine-usage messages
	// reference machines in "entrance-area" (there should be none).
	now := time.Now()
	for i := 0; i < 300; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}

	for _, msg := range pub.messages {
		if msg.topic == "machine-usage" {
			if mum, ok := msg.payload.(MachineUsageMessage); ok {
				// Find the machine and check its area.
				engine.mu.Lock()
				m := engine.findMachineByID(mum.MachineID)
				if m != nil && m.AreaID == "entrance-area" {
					t.Fatalf("machine %s should not be in ingresso", mum.MachineID)
				}
				engine.mu.Unlock()
			}
		}
	}
}

func TestBadgeIDFormat(t *testing.T) {
	tests := []struct {
		n    int
		want string
	}{
		{1, "badge-001"},
		{9, "badge-009"},
		{10, "badge-010"},
		{15, "badge-015"},
	}
	for _, tc := range tests {
		got := badgeID(tc.n)
		if got != tc.want {
			t.Errorf("badgeID(%d) = %q, want %q", tc.n, got, tc.want)
		}
	}
}

func TestAreaOccupancyConsistency(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(42))
	engine := NewEngine(pub, rng)

	now := time.Now()
	for i := 0; i < 500; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}

	// Track per-badge area state: which area each user is currently in.
	// After processing all messages, for users still in the gym they should
	// be in exactly one area; for users who exited, in no area.
	type badgeState struct {
		inGym       bool
		currentArea string
	}
	badges := make(map[string]*badgeState)

	for _, msg := range pub.messages {
		switch msg.topic {
		case "gym-access":
			gam := msg.payload.(GymAccessMessage)
			bs, ok := badges[gam.BadgeID]
			if !ok {
				bs = &badgeState{}
				badges[gam.BadgeID] = bs
			}
			if gam.AccessType == "ENTRY" {
				if bs.inGym {
					t.Fatalf("badge %s got ENTRY while already in gym", gam.BadgeID)
				}
				bs.inGym = true
			} else {
				if !bs.inGym {
					t.Fatalf("badge %s got EXIT while not in gym", gam.BadgeID)
				}
				if bs.currentArea != "" {
					t.Fatalf("badge %s got EXIT but still in area %s", gam.BadgeID, bs.currentArea)
				}
				bs.inGym = false
			}
		case "area-access":
			aam := msg.payload.(AreaAccessMessage)
			bs, ok := badges[aam.BadgeID]
			if !ok {
				bs = &badgeState{}
				badges[aam.BadgeID] = bs
			}
			if aam.Direction == "IN" {
				if bs.currentArea != "" {
					t.Fatalf("badge %s entered area %s but still in area %s",
						aam.BadgeID, aam.AreaID, bs.currentArea)
				}
				bs.currentArea = aam.AreaID
			} else {
				if bs.currentArea != aam.AreaID {
					t.Fatalf("badge %s exited area %s but was in area %s",
						aam.BadgeID, aam.AreaID, bs.currentArea)
				}
				bs.currentArea = ""
			}
		}
	}

	// All users still in the gym must be in exactly one area.
	for badgeID, bs := range badges {
		if bs.inGym && bs.currentArea == "" {
			t.Fatalf("badge %s is in gym but not in any area", badgeID)
		}
	}
}

func TestUsersAppearInIngressoAndCorsi(t *testing.T) {
	pub := &fakePublisher{}
	rng := rand.New(rand.NewSource(42))
	engine := NewEngine(pub, rng)

	now := time.Now()
	for i := 0; i < 500; i++ {
		engine.BusinessStep(now.Add(time.Duration(i) * 5 * time.Second))
	}

	seenAreas := make(map[string]bool)
	for _, msg := range pub.messages {
		if msg.topic == "area-access" {
			aam := msg.payload.(AreaAccessMessage)
			if aam.Direction == "IN" {
				seenAreas[aam.AreaID] = true
			}
		}
	}

	if !seenAreas["entrance-area"] {
		t.Fatal("no user ever appeared in area 'ingresso'")
	}
	if !seenAreas["class-area"] {
		t.Fatal("no user ever appeared in area 'corsi'")
	}
}
