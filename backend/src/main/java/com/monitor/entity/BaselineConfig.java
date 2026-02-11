package com.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "baseline_config")
public class BaselineConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false, length = 36)
    private String agentId;

    @Column(name = "baseline_type", nullable = false, length = 50)
    private String baselineType;

    @Column(length = 20)
    private String status;

    @Column(name = "learning_mode", length = 20)
    private String learningMode;

    @Column(name = "learning_days")
    private Integer learningDays;

    @Column(name = "learn_start")
    private LocalDateTime learnStart;

    @Column(name = "learn_end")
    private LocalDateTime learnEnd;

    @Column(length = 20)
    private String sensitivity;

    @Column(name = "alert_enabled")
    private Boolean alertEnabled;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_type", length = 20)
    private String createdType;

    @Column(name = "source_agent_id", length = 36)
    private String sourceAgentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "LEARNING";
        if (learningMode == null) learningMode = "STANDARD";
        if (learningDays == null) learningDays = 7;
        if (sensitivity == null) sensitivity = "MEDIUM";
        if (alertEnabled == null) alertEnabled = true;
        if (createdType == null) createdType = "LEARNING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getBaselineType() { return baselineType; }
    public void setBaselineType(String baselineType) { this.baselineType = baselineType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLearningMode() { return learningMode; }
    public void setLearningMode(String learningMode) { this.learningMode = learningMode; }

    public Integer getLearningDays() { return learningDays; }
    public void setLearningDays(Integer learningDays) { this.learningDays = learningDays; }

    public LocalDateTime getLearnStart() { return learnStart; }
    public void setLearnStart(LocalDateTime learnStart) { this.learnStart = learnStart; }

    public LocalDateTime getLearnEnd() { return learnEnd; }
    public void setLearnEnd(LocalDateTime learnEnd) { this.learnEnd = learnEnd; }

    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }

    public Boolean getAlertEnabled() { return alertEnabled; }
    public void setAlertEnabled(Boolean alertEnabled) { this.alertEnabled = alertEnabled; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedType() { return createdType; }
    public void setCreatedType(String createdType) { this.createdType = createdType; }

    public String getSourceAgentId() { return sourceAgentId; }
    public void setSourceAgentId(String sourceAgentId) { this.sourceAgentId = sourceAgentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
