package com.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "current_processes")
public class CurrentProcessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 36, nullable = false)
    private String agentId;

    @Column(nullable = false)
    private Integer pid;

    @Column(length = 255)
    private String name;

    @Column(name = "cpu_percent")
    private Double cpuPercent;

    @Column(name = "memory_percent")
    private Double memoryPercent;

    @Column(length = 20)
    private String status;

    @Column(name = "create_time")
    private String createTime;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(Double cpuPercent) { this.cpuPercent = cpuPercent; }

    public Double getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(Double memoryPercent) { this.memoryPercent = memoryPercent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}
