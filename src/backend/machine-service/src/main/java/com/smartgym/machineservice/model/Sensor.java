package com.smartgym.machineservice.model;

import com.smartgym.machineservice.ddd.Entity;

public class Sensor implements Entity<String> {

    private String id;
    private String name;

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return this.id;
    }
}