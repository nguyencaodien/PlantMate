package com.example.ungdungchiasecaycanh.model;

/**
 * One Plant Doctor answer: what the plant probably is (optional), whether it looks healthy
 * (optional), the most likely issue, a 0–1 confidence (optional) and care guidance.
 */
public class PlantDiagnosisResult {
    private final String diagnosis;
    private final Double confidence;
    private final String guidance;
    private final String plantName;
    private final String scientificName;
    private final Boolean healthy;

    public PlantDiagnosisResult(String diagnosis, Double confidence, String guidance) {
        this(diagnosis, confidence, guidance, null, null, null);
    }

    public PlantDiagnosisResult(String diagnosis, Double confidence, String guidance,
                                String plantName, String scientificName, Boolean healthy) {
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.guidance = guidance;
        this.plantName = plantName;
        this.scientificName = scientificName;
        this.healthy = healthy;
    }

    public String getDiagnosis() { return diagnosis; }
    public Double getConfidence() { return confidence; }
    public String getGuidance() { return guidance; }
    public String getPlantName() { return plantName; }
    public String getScientificName() { return scientificName; }
    public Boolean getHealthy() { return healthy; }
}
