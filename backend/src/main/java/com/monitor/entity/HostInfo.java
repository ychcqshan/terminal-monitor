package com.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "host_info")
public class HostInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false, length = 36)
    private String agentId;

    @Column(name = "cpu_brand", length = 200)
    private String cpuBrand;

    @Column(name = "cpu_arch", length = 50)
    private String cpuArch;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "cpu_threads")
    private Integer cpuThreads;

    @Column(name = "cpu_frequency")
    private Double cpuFrequency;

    @Column(name = "memory_total")
    private Long memoryTotal;

    @Column(name = "memory_available")
    private Long memoryAvailable;

    @Column(name = "memory_percent")
    private Double memoryPercent;

    @Column(name = "memory_human", length = 50)
    private String memoryHuman;

    @Column(name = "storage_devices", columnDefinition = "JSON")
    private String storageDevices;

    @Column(name = "storage_total")
    private Long storageTotal;

    @Column(name = "motherboard_model", length = 200)
    private String motherboardModel;

    @Column(name = "motherboard_serial", length = 100)
    private String motherboardSerial;

    @Column(name = "bios_version", length = 100)
    private String biosVersion;

    @Column(name = "os_name", length = 200)
    private String osName;

    @Column(name = "os_version", length = 100)
    private String osVersion;

    @Column(name = "os_arch", length = 50)
    private String osArch;

    @Column(name = "kernel_version", length = 100)
    private String kernelVersion;

    @Column(name = "mac_addresses", columnDefinition = "JSON")
    private String macAddresses;

    @Column(name = "ip_addresses", columnDefinition = "JSON")
    private String ipAddresses;

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

    public String getCpuBrand() { return cpuBrand; }
    public void setCpuBrand(String cpuBrand) { this.cpuBrand = cpuBrand; }

    public String getCpuArch() { return cpuArch; }
    public void setCpuArch(String cpuArch) { this.cpuArch = cpuArch; }

    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }

    public Integer getCpuThreads() { return cpuThreads; }
    public void setCpuThreads(Integer cpuThreads) { this.cpuThreads = cpuThreads; }

    public Double getCpuFrequency() { return cpuFrequency; }
    public void setCpuFrequency(Double cpuFrequency) { this.cpuFrequency = cpuFrequency; }

    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }

    public Long getMemoryAvailable() { return memoryAvailable; }
    public void setMemoryAvailable(Long memoryAvailable) { this.memoryAvailable = memoryAvailable; }

    public Double getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(Double memoryPercent) { this.memoryPercent = memoryPercent; }

    public String getMemoryHuman() { return memoryHuman; }
    public void setMemoryHuman(String memoryHuman) { this.memoryHuman = memoryHuman; }

    public String getStorageDevices() { return storageDevices; }
    public void setStorageDevices(String storageDevices) { this.storageDevices = storageDevices; }

    public Long getStorageTotal() { return storageTotal; }
    public void setStorageTotal(Long storageTotal) { this.storageTotal = storageTotal; }

    public String getMotherboardModel() { return motherboardModel; }
    public void setMotherboardModel(String motherboardModel) { this.motherboardModel = motherboardModel; }

    public String getMotherboardSerial() { return motherboardSerial; }
    public void setMotherboardSerial(String motherboardSerial) { this.motherboardSerial = motherboardSerial; }

    public String getBiosVersion() { return biosVersion; }
    public void setBiosVersion(String biosVersion) { this.biosVersion = biosVersion; }

    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getOsArch() { return osArch; }
    public void setOsArch(String osArch) { this.osArch = osArch; }

    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }

    public String getMacAddresses() { return macAddresses; }
    public void setMacAddresses(String macAddresses) { this.macAddresses = macAddresses; }

    public String getIpAddresses() { return ipAddresses; }
    public void setIpAddresses(String ipAddresses) { this.ipAddresses = ipAddresses; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}
