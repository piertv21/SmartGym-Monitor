package com.smartgym.areaservice;

import com.smartgym.areaservice.application.AreaServiceAPIImpl;
import com.smartgym.areaservice.application.ports.AreaRepository;
import com.smartgym.areaservice.model.AreaAccessMessage;
import com.smartgym.areaservice.model.AreaType;
import com.smartgym.areaservice.model.GymArea;
import com.smartgym.areaservice.model.UpdateAreaCapacityMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

public class JUnitAreaServiceTest {

    @Test
    void processAreaAccessEntryIncrementsCurrentCount() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 0);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-cardio-01",
                "2026-03-26T10:00:00Z",
                "badge-001",
                "cardio-area",
                "IN"
        );

        areaService.processAreaAccess(message).join();

        GymArea updated = areaService.getAreaById("cardio-area").join().orElseThrow();
        assertEquals(1, updated.getCurrentCount());
    }

    @Test
    void processAreaAccessExitDecrementsCurrentCount() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 3);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-cardio-01",
                "2026-03-26T10:05:00Z",
                "badge-001",
                "cardio-area",
                "OUT"
        );

        areaService.processAreaAccess(message).join();

        GymArea updated = areaService.getAreaById("cardio-area").join().orElseThrow();
        assertEquals(2, updated.getCurrentCount());
    }

    @Test
    void processAreaExitDecrementsCurrentCount() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 3);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-cardio-01",
                "2026-03-26T10:05:00Z",
                "badge-001",
                "cardio-area",
                "OUT"
        );

        areaService.processAreaExit(message).join();

        GymArea updated = areaService.getAreaById("cardio-area").join().orElseThrow();
        assertEquals(2, updated.getCurrentCount());
    }

    @Test
    void processAreaAccessFailsWhenAreaDoesNotExist() {
        AreaRepository repository = new InMemoryAreaRepository(Map.of());
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-cardio-01",
                "2026-03-26T10:00:00Z",
                "badge-001",
                "missing-area",
                "IN"
        );

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> areaService.processAreaAccess(message).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Area not found"));
    }

    @Test
    void getAreaByIdReturnsAreaWhenPresent() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 4);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        Optional<GymArea> result = areaService.getAreaById("cardio-area").join();

        assertTrue(result.isPresent());
        assertEquals("cardio-area", result.get().getId());
        assertEquals("Cardio Zone", result.get().getName());
        assertEquals(4, result.get().getCurrentCount());
    }

    @Test
    void getAreaByIdReturnsEmptyWhenAreaIsMissing() {
        AreaRepository repository = new InMemoryAreaRepository(Map.of());
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        Optional<GymArea> result = areaService.getAreaById("missing-area").join();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAreasReturnsAllAreas() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 2);
        GymArea weight = new GymArea("weight-area", "Weight Zone", AreaType.WEIGHT, 15, 1);

        AreaRepository repository = new InMemoryAreaRepository(
                Map.of(
                        "cardio-area", cardio,
                        "weight-area", weight
                )
        );

        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        List<GymArea> result = areaService.getAllAreas().join();

        assertEquals(2, result.size());
    }

    @Test
    void updateAreaCapacityUpdatesCapacitySuccessfully() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 5);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        UpdateAreaCapacityMessage message = new UpdateAreaCapacityMessage("cardio-area", 30);

        areaService.updateAreaCapacity(message).join();

        GymArea updated = areaService.getAreaById("cardio-area").join().orElseThrow();
        assertEquals(30, updated.getCapacity());
    }

    @Test
    void updateAreaCapacityFailsWhenNewCapacityIsLowerThanCurrentCount() {
        GymArea cardio = new GymArea("cardio-area", "Cardio Zone", AreaType.CARDIO, 20, 10);
        AreaRepository repository = new InMemoryAreaRepository(Map.of("cardio-area", cardio));
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        UpdateAreaCapacityMessage message = new UpdateAreaCapacityMessage("cardio-area", 5);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> areaService.updateAreaCapacity(message).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("New capacity cannot be lower than current count"));
    }

    @Test
    void updateAreaCapacityFailsWhenAreaDoesNotExist() {
        AreaRepository repository = new InMemoryAreaRepository(Map.of());
        AreaServiceAPIImpl areaService = new AreaServiceAPIImpl(repository);

        UpdateAreaCapacityMessage message = new UpdateAreaCapacityMessage("missing-area", 10);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> areaService.updateAreaCapacity(message).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Area not found"));
    }

    private static final class InMemoryAreaRepository implements AreaRepository {

        private final Map<String, GymArea> areasById;

        private InMemoryAreaRepository(Map<String, GymArea> initialAreas) {
            this.areasById = new LinkedHashMap<>(initialAreas);
        }

        @Override
        public CompletableFuture<Void> saveArea(GymArea area) {
            areasById.put(area.getId(), area);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<GymArea>> findAreaById(String areaId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(areasById.get(areaId)));
        }

        @Override
        public CompletableFuture<List<GymArea>> findAllAreas() {
            return CompletableFuture.completedFuture(new ArrayList<>(areasById.values()));
        }
    }
}