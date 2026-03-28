package com.smartgym.embeddedservice.infrastructure.eventbus;

public final class EventBusAddresses {

    public static final String GYM_ACCESS = "event.gym-access";
    public static final String AREA_ACCESS = "event.area-access";
    public static final String MACHINE_USAGE = "event.machine-usage";
    public static final String DEVICE_STATUS = "event.device-status";

    private EventBusAddresses() {
    }
}