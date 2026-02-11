package com.monitor.service;

import com.monitor.entity.HostInfo;
import com.monitor.repository.HostInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HostInfoService {

    private static final Logger logger = LoggerFactory.getLogger(HostInfoService.class);

    private final HostInfoRepository hostInfoRepository;

    public HostInfoService(HostInfoRepository hostInfoRepository) {
        this.hostInfoRepository = hostInfoRepository;
    }

    @Transactional
    public HostInfo saveHostInfo(String agentId, Map<String, Object> data) {
        logger.debug("Saving host info for agent: {}", agentId);

        HostInfo hostInfo = new HostInfo();
        hostInfo.setAgentId(agentId);

        if (data.containsKey("cpu_brand")) {
            hostInfo.setCpuBrand(getStringValue(data.get("cpu_brand")));
        }
        if (data.containsKey("cpu_arch")) {
            hostInfo.setCpuArch(getStringValue(data.get("cpu_arch")));
        }
        if (data.containsKey("cpu_cores")) {
            hostInfo.setCpuCores(getIntValue(data.get("cpu_cores")));
        }
        if (data.containsKey("cpu_threads")) {
            hostInfo.setCpuThreads(getIntValue(data.get("cpu_threads")));
        }
        if (data.containsKey("cpu_frequency")) {
            hostInfo.setCpuFrequency(getDoubleValue(data.get("cpu_frequency")));
        }
        if (data.containsKey("memory_total")) {
            hostInfo.setMemoryTotal(getLongValue(data.get("memory_total")));
        }
        if (data.containsKey("memory_available")) {
            hostInfo.setMemoryAvailable(getLongValue(data.get("memory_available")));
        }
        if (data.containsKey("memory_percent")) {
            hostInfo.setMemoryPercent(getDoubleValue(data.get("memory_percent")));
        }
        if (data.containsKey("memory_human")) {
            hostInfo.setMemoryHuman(getStringValue(data.get("memory_human")));
        }
        if (data.containsKey("storage_devices")) {
            hostInfo.setStorageDevices(getStringValue(data.get("storage_devices")));
        }
        if (data.containsKey("storage_total")) {
            hostInfo.setStorageTotal(getLongValue(data.get("storage_total")));
        }
        if (data.containsKey("motherboard_model")) {
            hostInfo.setMotherboardModel(getStringValue(data.get("motherboard_model")));
        }
        if (data.containsKey("motherboard_serial")) {
            hostInfo.setMotherboardSerial(getStringValue(data.get("motherboard_serial")));
        }
        if (data.containsKey("bios_version")) {
            hostInfo.setBiosVersion(getStringValue(data.get("bios_version")));
        }
        if (data.containsKey("os_name")) {
            hostInfo.setOsName(getStringValue(data.get("os_name")));
        }
        if (data.containsKey("os_version")) {
            hostInfo.setOsVersion(getStringValue(data.get("os_version")));
        }
        if (data.containsKey("os_arch")) {
            hostInfo.setOsArch(getStringValue(data.get("os_arch")));
        }
        if (data.containsKey("kernel_version")) {
            hostInfo.setKernelVersion(getStringValue(data.get("kernel_version")));
        }
        if (data.containsKey("mac_addresses")) {
            hostInfo.setMacAddresses(getStringValue(data.get("mac_addresses")));
        }
        if (data.containsKey("ip_addresses")) {
            hostInfo.setIpAddresses(getStringValue(data.get("ip_addresses")));
        }

        HostInfo saved = hostInfoRepository.saveAndFlush(hostInfo);
        logger.debug("Host info saved with id: {}", saved.getId());
        return saved;
    }

    public List<HostInfo> getHostInfoHistory(String agentId) {
        return hostInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
    }

    public Optional<HostInfo> getLatestHostInfo(String agentId) {
        return Optional.ofNullable(hostInfoRepository.findTopByAgentIdOrderByCollectedAtDesc(agentId));
    }

    @Transactional
    public void deleteByAgentId(String agentId) {
        hostInfoRepository.deleteByAgentId(agentId);
        logger.info("Deleted host info for agent: {}", agentId);
    }

    private Integer getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Long getLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String getStringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
