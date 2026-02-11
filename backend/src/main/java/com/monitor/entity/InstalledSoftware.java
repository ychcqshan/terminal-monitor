package com.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installed_software")
public class InstalledSoftware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false, length = 36)
    private String agentId;

    @Column(name = "software_name", length = 500)
    private String softwareName;

    @Column(length = 100)
    private String version;

    @Column(length = 200)
    private String publisher;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "install_location", length = 500)
    private String installLocation;

    @Column
    private Integer size;

    @Column(name = "software_type", length = 50)
    private String softwareType;

    @Column(length = 50)
    private String source;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getSoftwareName() { return softwareName; }
    public void setSoftwareName(String softwareName) { this.softwareName = softwareName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }

    public String getInstallLocation() { return installLocation; }
    public void setInstallLocation(String installLocation) { this.installLocation = installLocation; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSoftwareType() { return softwareType; }
    public void setSoftwareType(String softwareType) { this.softwareType = softwareType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}
