package com.airflow.centralbackend.dto;

public class Manifest {
    private String company_name;
    private String dispatcher_name;
    private Coordinate location;
    private String task_type;
    private String flight_number;
    private String pickup_time;
    private String priority;

    public Manifest() {}

    public String getCompany_name() {
        return company_name;
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public String getDispatcher_name() {
        return dispatcher_name;
    }

    public void setDispatcher_name(String dispatcher_name) {
        this.dispatcher_name = dispatcher_name;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }

    public String getTask_type() {
        return task_type;
    }

    public void setTask_type(String task_type) {
        this.task_type = task_type;
    }

    public String getFlight_number() {
        return flight_number;
    }

    public void setFlight_number(String flight_number) {
        this.flight_number = flight_number;
    }

    public String getPickup_time() {
        return pickup_time;
    }

    public void setPickup_time(String pickup_time) {
        this.pickup_time = pickup_time;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
