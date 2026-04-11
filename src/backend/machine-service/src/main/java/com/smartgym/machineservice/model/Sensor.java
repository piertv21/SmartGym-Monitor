package com.smartgym.machineservice.model;

import com.smartgym.machineservice.ddd.Entity;

public class Sensor implements Entity<String> {

    private final String id;

    public Sensor(String id) {
        this.id = id;
    }


    @Override
    public String getId() {
        return this.id;
    }
}