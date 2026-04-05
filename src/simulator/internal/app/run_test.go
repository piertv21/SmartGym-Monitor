package app

import (
	"context"
	"sync/atomic"
	"testing"
	"time"
)

func TestRunSupervisedWorkerRestartsOnPanic(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var calls atomic.Int32

	done := make(chan struct{})
	go func() {
		runSupervisedWorker(ctx, "test-worker", func(context.Context) {
			if calls.Add(1) == 1 {
				panic("boom")
			}
			cancel()
		})
		close(done)
	}()

	select {
	case <-done:
	case <-time.After(3 * time.Second):
		t.Fatal("worker did not finish")
	}

	if calls.Load() < 2 {
		t.Fatalf("expected restart, calls=%d", calls.Load())
	}
}
