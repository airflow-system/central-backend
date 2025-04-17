package com.airflow.centralbackend.dto;

import java.util.List;

public class ManifestsResponse {
    private List<Manifest> manifests;
    public ManifestsResponse() {}
    public List<Manifest> getManifests() { return manifests; }
    public void setManifests(List<Manifest> manifests) { this.manifests = manifests; }
}