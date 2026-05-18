package com.cirm.platform.ai.port;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * DTO representing the response returned by the Python AI service.
 *
 * This is NOT a domain object and is used only for communication
 * between the Java backend and the AI microservice.
 */
public class AiClassificationResponse {

    @JsonAlias("intent")
    private String type;

    @JsonAlias("department")
    private String category;

    private String intent;
    private String department;
    private String priority;
    private double confidence;

    public AiClassificationResponse() {}

    public String getType() { return type != null ? type : intent; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category != null ? category : department; }
    public void setCategory(String category) { this.category = category; }

    public String getIntent() { return getType(); }
    public void setIntent(String intent) { this.intent = intent; }

    public String getDepartment() { return getCategory(); }
    public void setDepartment(String department) { this.department = department; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
