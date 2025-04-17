package com.airflow.centralbackend.dto;

public class IntersectionResponse {
    private boolean accident;
    private int speed;
    public IntersectionResponse() {}
    public boolean isAccident() { return accident; }
    public void setAccident(boolean accident) { this.accident = accident; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
}