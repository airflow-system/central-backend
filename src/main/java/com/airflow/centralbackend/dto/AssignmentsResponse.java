package com.airflow.centralbackend.dto;


import java.util.List;

public class AssignmentsResponse {
    private List<Assignment> assignments;
    public AssignmentsResponse() {}
    public List<Assignment> getAssignments() { return assignments; }
    public void setAssignments(List<Assignment> assignments) { this.assignments = assignments; }
}