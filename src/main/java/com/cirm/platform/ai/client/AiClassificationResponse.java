package com.cirm.platform.ai.client;

/**
 * DTO representing the response returned by the Python AI service.
 *
 * This is NOT a domain object and is used only for communication
 * between the Java backend and the AI microservice.
 */
public class AiClassificationResponse {

    private String intent;
    private String department;
    private String priority;
    private double confidence;

    public AiClassificationResponse() {}

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
