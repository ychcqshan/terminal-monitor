package com.monitor.service;

import com.monitor.entity.Agent;
import com.monitor.entity.HostInfo;
import com.monitor.entity.PortInfo;
import com.monitor.entity.ProcessInfo;
import com.monitor.entity.InstalledSoftware;
import com.monitor.entity.UsbDevice;
import com.monitor.entity.LoginLog;
import com.monitor.repository.AgentRepository;
import com.monitor.repository.HostInfoRepository;
import com.monitor.repository.PortInfoRepository;
import com.monitor.repository.ProcessInfoRepository;
import com.monitor.repository.InstalledSoftwareRepository;
import com.monitor.repository.UsbDeviceRepository;
import com.monitor.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final ProcessInfoRepository processInfoRepository;
    private final PortInfoRepository portInfoRepository;
    private final HostInfoRepository hostInfoRepository;
    private final InstalledSoftwareRepository installedSoftwareRepository;
    private final UsbDeviceRepository usbDeviceRepository;
    private final LoginLogRepository loginLogRepository;

    public AgentService(AgentRepository agentRepository,
                       ProcessInfoRepository processInfoRepository,
                       PortInfoRepository portInfoRepository,
                       HostInfoRepository hostInfoRepository,
                       InstalledSoftwareRepository installedSoftwareRepository,
                       UsbDeviceRepository usbDeviceRepository,
                       LoginLogRepository loginLogRepository) {
        this.agentRepository = agentRepository;
        this.processInfoRepository = processInfoRepository;
        this.portInfoRepository = portInfoRepository;
        this.hostInfoRepository = hostInfoRepository;
        this.installedSoftwareRepository = installedSoftwareRepository;
        this.usbDeviceRepository = usbDeviceRepository;
        this.loginLogRepository = loginLogRepository;
    }

    public Agent registerOrUpdateAgent(Map<String, String> agentInfo) {
        String agentId = agentInfo.get("agentId");
        logger.debug("registerOrUpdateAgent called with agentId: {}", agentId);
        Optional<Agent> existingAgent = agentRepository.findById(agentId);

        Agent agent;
        if (existingAgent.isPresent()) {
            agent = existingAgent.get();
            agent.setName(agentInfo.getOrDefault("name", agent.getName()));
            agent.setPlatform(agentInfo.getOrDefault("platform", agent.getPlatform()));
            agent.setHostname(agentInfo.getOrDefault("hostname", agent.getHostname()));
            agent.setIpAddress(agentInfo.getOrDefault("ipAddress", agent.getIpAddress()));
            agent.setStatus("online");
        } else {
            agent = new Agent();
            agent.setId(agentId);
            agent.setName(agentInfo.getOrDefault("name", "Unknown"));
            agent.setPlatform(agentInfo.getOrDefault("platform", "Unknown"));
            agent.setHostname(agentInfo.getOrDefault("hostname", "Unknown"));
            agent.setIpAddress(agentInfo.getOrDefault("ipAddress", "Unknown"));
            agent.setStatus("online");
        }

        Agent saved = agentRepository.saveAndFlush(agent);
        logger.debug("Agent saved with id: {}", saved.getId());
        return saved;
    }

    public boolean updateHeartbeat(String agentId, String status) {
        logger.debug("updateHeartbeat called with agentId: {}, status: {}", agentId, status);
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        logger.debug("Agent found: {}", agentOpt.isPresent());
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setStatus(status);
            agent.setUpdatedAt(LocalDateTime.now());
            agentRepository.saveAndFlush(agent);
            logger.debug("Heartbeat updated successfully for agent: {}", agentId);
            return true;
        }
        logger.warn("Heartbeat update failed: Agent not found with id {}", agentId);
        return false;
    }

    public Optional<Agent> getAgent(String agentId) {
        return agentRepository.findById(agentId);
    }

    @Transactional
    public boolean updateAgent(String agentId, Map<String, String> agentInfo) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            if (agentInfo.containsKey("name")) {
                agent.setName(agentInfo.get("name"));
            }
            if (agentInfo.containsKey("status")) {
                agent.setStatus(agentInfo.get("status"));
            }
            agentRepository.saveAndFlush(agent);
            logger.debug("Agent updated: {}", agentId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteAgent(String agentId) {
        if (agentRepository.existsById(agentId)) {
            processInfoRepository.deleteByAgentId(agentId);
            portInfoRepository.deleteByAgentId(agentId);
            hostInfoRepository.deleteByAgentId(agentId);
            installedSoftwareRepository.deleteByAgentId(agentId);
            usbDeviceRepository.deleteByAgentId(agentId);
            loginLogRepository.deleteByAgentId(agentId);
            agentRepository.deleteById(agentId);
            logger.info("Agent deleted: {}", agentId);
            return true;
        }
        return false;
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public List<Agent> getAgentsByStatus(String status) {
        return agentRepository.findByStatus(status);
    }

    public long countOnlineAgents() {
        return agentRepository.countByStatus("online");
    }

    public long countOfflineAgents() {
        return agentRepository.countByStatus("offline");
    }

    @Transactional
    public void saveMonitorData(String agentId, Map<String, Object> data) {
        logger.info("========== Saving monitor data for agent: {} ==========", agentId);

        @SuppressWarnings("unchecked")
        Map<String, Object> nestedData = (Map<String, Object>) data.get("data");
        if (nestedData == null) {
            logger.warn("Payload for agent {} does not contain a 'data' object.", agentId);
            return;
        }

        Object processesObj = nestedData.get("processes");
        Object portsObj = nestedData.get("ports");
        Object hostInfoObj = nestedData.get("host_info");
        Object installedSoftwareObj = nestedData.get("installed_software");
        Object usbDevicesObj = nestedData.get("usb_devices");
        Object loginLogsObj = nestedData.get("login_logs");

        if (processesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> processes = (List<Map<String, Object>>) processesObj;
            logger.debug("---------- Process Data ({} total) ----------", processes.size());
            for (int i = 0; i < Math.min(processes.size(), 10); i++) {
                Map<String, Object> proc = processes.get(i);
                logger.debug("[{}] PID:{}, Name:{}, CPU:{}%, Memory:{}%, Status:{}",
                    i + 1,
                    proc.get("pid"),
                    proc.get("name"),
                    proc.get("cpu_percent"),
                    proc.get("memory_percent"),
                    proc.get("status"));
            }
            if (processes.size() > 10) {
                logger.debug("... and {} more processes", processes.size() - 10);
            }

            for (Map<String, Object> proc : processes) {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.setAgentId(agentId);
                processInfo.setPid(getIntValue(proc.get("pid")));
                processInfo.setName(getStringValue(proc.get("name")));
                processInfo.setCpuPercent(getDoubleValue(proc.get("cpu_percent")));
                processInfo.setMemoryPercent(getDoubleValue(proc.get("memory_percent")));
                processInfo.setStatus(getStringValue(proc.get("status")));
                processInfo.setCreateTime(getStringValue(proc.get("create_time")));
                processInfoRepository.saveAndFlush(processInfo);
            }
            logger.debug("Finished saving {} process entries.", processes.size());
        } else {
            logger.debug("No process data received for agent: {}", agentId);
        }

        if (portsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ports = (List<Map<String, Object>>) portsObj;
            logger.debug("---------- Port Data ({} total) ----------", ports.size());
            for (int i = 0; i < Math.min(ports.size(), 10); i++) {
                Map<String, Object> port = ports.get(i);
                logger.debug("[{}] Port:{}, Protocol:{}, Status:{}, Process:{}",
                    i + 1,
                    port.get("port"),
                    port.get("protocol"),
                    port.get("status"),
                    port.get("process_name"));
            }
            if (ports.size() > 10) {
                logger.debug("... and {} more ports", ports.size() - 10);
            }

            for (Map<String, Object> port : ports) {
                PortInfo portInfo = new PortInfo();
                portInfo.setAgentId(agentId);
                portInfo.setPort(getIntValue(port.get("port")));
                portInfo.setProtocol(getStringValue(port.get("protocol")));
                portInfo.setStatus(getStringValue(port.get("status")));
                portInfo.setPid(getIntValue(port.get("pid")));
                portInfo.setProcessName(getStringValue(port.get("process_name")));
                portInfoRepository.saveAndFlush(portInfo);
            }
            logger.debug("Finished saving {} port entries.", ports.size());
        } else {
            logger.debug("No port data received for agent: {}", agentId);
        }

        if (hostInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hostInfoData = (Map<String, Object>) hostInfoObj;
            logger.debug("---------- Host Info Data ----------");
            HostInfo hostInfo = new HostInfo();
            hostInfo.setAgentId(agentId);

            if (hostInfoData.containsKey("cpu_brand")) {
                hostInfo.setCpuBrand(getStringValue(hostInfoData.get("cpu_brand")));
            }
            if (hostInfoData.containsKey("cpu_arch")) {
                hostInfo.setCpuArch(getStringValue(hostInfoData.get("cpu_arch")));
            }
            if (hostInfoData.containsKey("cpu_cores")) {
                hostInfo.setCpuCores(getIntValue(hostInfoData.get("cpu_cores")));
            }
            if (hostInfoData.containsKey("cpu_threads")) {
                hostInfo.setCpuThreads(getIntValue(hostInfoData.get("cpu_threads")));
            }
            if (hostInfoData.containsKey("cpu_frequency")) {
                hostInfo.setCpuFrequency(getDoubleValue(hostInfoData.get("cpu_frequency")));
            }
            if (hostInfoData.containsKey("memory_total")) {
                hostInfo.setMemoryTotal(getLongValue(hostInfoData.get("memory_total")));
            }
            if (hostInfoData.containsKey("memory_available")) {
                hostInfo.setMemoryAvailable(getLongValue(hostInfoData.get("memory_available")));
            }
            if (hostInfoData.containsKey("memory_percent")) {
                hostInfo.setMemoryPercent(getDoubleValue(hostInfoData.get("memory_percent")));
            }
            if (hostInfoData.containsKey("memory_human")) {
                hostInfo.setMemoryHuman(getStringValue(hostInfoData.get("memory_human")));
            }
            if (hostInfoData.containsKey("storage_devices")) {
                hostInfo.setStorageDevices(getStringValue(hostInfoData.get("storage_devices")));
            }
            if (hostInfoData.containsKey("storage_total")) {
                hostInfo.setStorageTotal(getLongValue(hostInfoData.get("storage_total")));
            }
            if (hostInfoData.containsKey("motherboard_model")) {
                hostInfo.setMotherboardModel(getStringValue(hostInfoData.get("motherboard_model")));
            }
            if (hostInfoData.containsKey("motherboard_serial")) {
                hostInfo.setMotherboardSerial(getStringValue(hostInfoData.get("motherboard_serial")));
            }
            if (hostInfoData.containsKey("bios_version")) {
                hostInfo.setBiosVersion(getStringValue(hostInfoData.get("bios_version")));
            }
            if (hostInfoData.containsKey("os_name")) {
                hostInfo.setOsName(getStringValue(hostInfoData.get("os_name")));
            }
            if (hostInfoData.containsKey("os_version")) {
                hostInfo.setOsVersion(getStringValue(hostInfoData.get("os_version")));
            }
            if (hostInfoData.containsKey("os_arch")) {
                hostInfo.setOsArch(getStringValue(hostInfoData.get("os_arch")));
            }
            if (hostInfoData.containsKey("kernel_version")) {
                hostInfo.setKernelVersion(getStringValue(hostInfoData.get("kernel_version")));
            }
            if (hostInfoData.containsKey("mac_addresses")) {
                hostInfo.setMacAddresses(getStringValue(hostInfoData.get("mac_addresses")));
            }
            if (hostInfoData.containsKey("ip_addresses")) {
                hostInfo.setIpAddresses(getStringValue(hostInfoData.get("ip_addresses")));
            }

            hostInfoRepository.saveAndFlush(hostInfo);
            logger.debug("Finished saving host info entry.");
        } else {
            logger.debug("No host info data received for agent: {}", agentId);
        }

        if (installedSoftwareObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> installedSoftwareList = (List<Map<String, Object>>) installedSoftwareObj;
            logger.debug("---------- Installed Software Data ({} total) ----------", installedSoftwareList.size());

            for (Map<String, Object> sw : installedSoftwareList) {
                InstalledSoftware software = new InstalledSoftware();
                software.setAgentId(agentId);

                if (sw.containsKey("software_name")) {
                    software.setSoftwareName(getStringValue(sw.get("software_name")));
                }
                if (sw.containsKey("software_type")) {
                    software.setSoftwareType(getStringValue(sw.get("software_type")));
                }
                if (sw.containsKey("version")) {
                    software.setVersion(getStringValue(sw.get("version")));
                }
                if (sw.containsKey("publisher")) {
                    software.setPublisher(getStringValue(sw.get("publisher")));
                }
                if (sw.containsKey("install_date")) {
                    software.setInstallDate(parseLocalDate(sw.get("install_date")));
                }
                if (sw.containsKey("install_location")) {
                    software.setInstallLocation(getStringValue(sw.get("install_location")));
                }
                if (sw.containsKey("size")) {
                    software.setSize(getIntValue(sw.get("size")));
                }
                if (sw.containsKey("source")) {
                    software.setSource(getStringValue(sw.get("source")));
                }

                installedSoftwareRepository.saveAndFlush(software);
            }
            logger.debug("Finished saving {} installed software entries.", installedSoftwareList.size());
        } else {
            logger.debug("No installed software data received for agent: {}", agentId);
        }

        if (usbDevicesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> usbDevicesList = (List<Map<String, Object>>) usbDevicesObj;
            logger.debug("---------- USB Devices Data ({} total) ----------", usbDevicesList.size());

            for (Map<String, Object> usb : usbDevicesList) {
                UsbDevice usbDevice = new UsbDevice();
                usbDevice.setAgentId(agentId);

                if (usb.containsKey("device_name")) {
                    usbDevice.setDeviceName(getStringValue(usb.get("device_name")));
                }
                if (usb.containsKey("device_type")) {
                    usbDevice.setDeviceType(getStringValue(usb.get("device_type")));
                }
                if (usb.containsKey("vendor_id")) {
                    usbDevice.setVendorId(getStringValue(usb.get("vendor_id")));
                }
                if (usb.containsKey("product_id")) {
                    usbDevice.setProductId(getStringValue(usb.get("product_id")));
                }
                if (usb.containsKey("serial_number")) {
                    usbDevice.setSerialNumber(getStringValue(usb.get("serial_number")));
                }
                if (usb.containsKey("manufacturer")) {
                    usbDevice.setManufacturer(getStringValue(usb.get("manufacturer")));
                }
                if (usb.containsKey("plugged_time")) {
                    usbDevice.setPluggedTime(parseLocalDateTime(usb.get("plugged_time")));
                }

                usbDeviceRepository.saveAndFlush(usbDevice);
            }
            logger.debug("Finished saving {} USB device entries.", usbDevicesList.size());
        } else {
            logger.debug("No USB devices data received for agent: {}", agentId);
        }

        if (loginLogsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> loginLogsList = (List<Map<String, Object>>) loginLogsObj;
            logger.debug("---------- Login Logs Data ({} total) ----------", loginLogsList.size());

            for (Map<String, Object> log : loginLogsList) {
                LoginLog loginLog = new LoginLog();
                loginLog.setAgentId(agentId);

                if (log.containsKey("username")) {
                    loginLog.setUsername(getStringValue(log.get("username")));
                }
                if (log.containsKey("login_type")) {
                    loginLog.setLoginType(getStringValue(log.get("login_type")));
                }
                if (log.containsKey("login_time")) {
                    loginLog.setLoginTime(parseLocalDateTime(log.get("login_time")));
                }
                if (log.containsKey("logout_time")) {
                    loginLog.setLogoutTime(parseLocalDateTime(log.get("logout_time")));
                }
                if (log.containsKey("login_ip")) {
                    loginLog.setLoginIp(getStringValue(log.get("login_ip")));
                }
                if (log.containsKey("login_status")) {
                    loginLog.setLoginStatus(getStringValue(log.get("login_status")));
                }
                if (log.containsKey("session_id")) {
                    loginLog.setSessionId(getStringValue(log.get("session_id")));
                }
                if (log.containsKey("source")) {
                    loginLog.setSource(getStringValue(log.get("source")));
                }

                loginLogRepository.saveAndFlush(loginLog);
            }
            logger.debug("Finished saving {} login log entries.", loginLogsList.size());
        } else {
            logger.debug("No login logs data received for agent: {}", agentId);
        }

        logger.info("========== Finished saving data for agent: {} ==========", agentId);
    }

    public List<ProcessInfo> getProcesses(String agentId) {
        return processInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
    }

    public List<PortInfo> getPorts(String agentId) {
        return portInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
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

    private java.time.LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String dateStr = value.toString();
        try {
            return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex) {
                logger.warn("Unable to parse date: {}", dateStr);
                return null;
            }
        }
    }

    private java.time.LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String dateTimeStr = value.toString();
        try {
            return java.time.LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return java.time.LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                logger.warn("Unable to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
    }
}
