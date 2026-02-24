package com.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "process_baseline")
public class ProcessBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 36, nullable = false)
    private String agentId;

    @Column(name = "process_name", length = 255, nullable = false)
    private String processName;

    @Column(precision = 5)
    private Double frequency;

    @Column(name = "frequency_category", length = 20)
    private String frequencyCategory;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "total_appearances")
    private Integer totalAppearances;

    @Column(name = "avg_cpu_percent", precision = 5)
    private Double avgCpuPercent;

    @Column(name = "avg_memory_percent", precision = 5)
    private Double avgMemoryPercent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public Double getFrequency() { return frequency; }
    public void setFrequency(Double frequency) { this.frequency = frequency; }

    public String getFrequencyCategory() { return frequencyCategory; }
    public void setFrequencyCategory(String frequencyCategory) { this.frequencyCategory = frequencyCategory; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Integer getTotalAppearances() { return totalAppearances; }
    public void setTotalAppearances(Integer totalAppearances) { this.totalAppearances = totalAppearances; }

    public Double getAvgCpuPercent() { return avgCpuPercent; }
    public void setAvgCpuPercent(Double avgCpuPercent) { this.avgCpuPercent = avgCpuPercent; }

    public Double getAvgMemoryPercent() { return avgMemoryPercent; }
    public void setAvgMemoryPercent(Double avgMemoryPercent) { this.avgMemoryPercent = avgMemoryPercent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
