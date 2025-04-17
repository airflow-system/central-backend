package com.airflow.centralbackend.dto;


public class Assignment {
    private String id;
    private String company_name;
    private String dispatcher_name;
    private Coordinate location;
    private String task_type;
    private String flight_number;
    private String pickup_time;
    private String priority;
    private String trucker_name;
    private String truck_id;

    public Assignment() {}
    // getters + setters for all fields...

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTruck_id() {
        return truck_id;
    }

    public void setTruck_id(String truck_id) {
        this.truck_id = truck_id;
    }

    public String getTrucker_name() {
        return trucker_name;
    }

    public void setTrucker_name(String trucker_name) {
        this.trucker_name = trucker_name;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPickup_time() {
        return pickup_time;
    }

    public void setPickup_time(String pickup_time) {
        this.pickup_time = pickup_time;
    }

    public String getFlight_number() {
        return flight_number;
    }

    public void setFlight_number(String flight_number) {
        this.flight_number = flight_number;
    }

    public String getTask_type() {
        return task_type;
    }

    public void setTask_type(String task_type) {
        this.task_type = task_type;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }

    public String getDispatcher_name() {
        return dispatcher_name;
    }

    public void setDispatcher_name(String dispatcher_name) {
        this.dispatcher_name = dispatcher_name;
    }

    public String getCompany_name() {
        return company_name;
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }
}