package com.airflow.centralbackend.dto;

import java.util.List;

/**
 * We’ll accept a list of Manifest objects,
 * then serialize to JSON in the controller and
 * send it as the `manifests_json` query‐param.
 */
public class AssignTasksRequest {
    private List<Manifest> manifests;
    public AssignTasksRequest() {}
    public List<Manifest> getManifests() { return manifests; }
    public void setManifests(List<Manifest> manifests) { this.manifests = manifests; }
}
