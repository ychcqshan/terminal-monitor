package com.monitor.service;

import com.monitor.entity.*;
import com.monitor.repository.*;
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
    private final AnomalyDetectionService anomalyDetectionService;
    private final CollectionRoundService collectionRoundService;
    private final CurrentProcessInfoRepository currentProcessInfoRepository;
    private final CurrentPortInfoRepository currentPortInfoRepository;
    private final ProcessHistoryRepository processHistoryRepository;
    private final PortHistoryRepository portHistoryRepository;

    public AgentService(AgentRepository agentRepository,
                       ProcessInfoRepository processInfoRepository,
                       PortInfoRepository portInfoRepository,
                       HostInfoRepository hostInfoRepository,
                       InstalledSoftwareRepository installedSoftwareRepository,
                       UsbDeviceRepository usbDeviceRepository,
                       LoginLogRepository loginLogRepository,
                       AnomalyDetectionService anomalyDetectionService,
                       CollectionRoundService collectionRoundService,
                       CurrentProcessInfoRepository currentProcessInfoRepository,
                       CurrentPortInfoRepository currentPortInfoRepository,
                       ProcessHistoryRepository processHistoryRepository,
                       PortHistoryRepository portHistoryRepository) {
        this.agentRepository = agentRepository;
        this.processInfoRepository = processInfoRepository;
        this.portInfoRepository = portInfoRepository;
        this.hostInfoRepository = hostInfoRepository;
        this.installedSoftwareRepository = installedSoftwareRepository;
        this.usbDeviceRepository = usbDeviceRepository;
        this.loginLogRepository = loginLogRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.collectionRoundService = collectionRoundService;
        this.currentProcessInfoRepository = currentProcessInfoRepository;
        this.currentPortInfoRepository = currentPortInfoRepository;
        this.processHistoryRepository = processHistoryRepository;
        this.portHistoryRepository = portHistoryRepository;
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

        int currentRound = collectionRoundService.getNextRound(agentId);
        logger.info("Current collection round for agent {}: {}", agentId, currentRound);

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

        // Process data - dual write
        if (processesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> processes = (List<Map<String, Object>>) processesObj;
            logger.debug("---------- Process Data ({} total) ----------", processes.size());
            
            // Save to current status table (replace)
            currentProcessInfoRepository.deleteByAgentId(agentId);
            for (Map<String, Object> proc : processes) {
                CurrentProcessInfo currentInfo = new CurrentProcessInfo();
                currentInfo.setAgentId(agentId);
                currentInfo.setPid(getIntValue(proc.get("pid")));
                currentInfo.setName(getStringValue(proc.get("name")));
                currentInfo.setCpuPercent(getDoubleValue(proc.get("cpu_percent")));
                currentInfo.setMemoryPercent(getDoubleValue(proc.get("memory_percent")));
                currentInfo.setStatus(getStringValue(proc.get("status")));
                currentInfo.setCreateTime(getStringValue(proc.get("create_time")));
                currentProcessInfoRepository.save(currentInfo);
            }
            
            // Save to history table (append)
            for (Map<String, Object> proc : processes) {
                ProcessHistory history = new ProcessHistory();
                history.setAgentId(agentId);
                history.setCollectionRound(currentRound);
                history.setPid(getIntValue(proc.get("pid")));
                history.setName(getStringValue(proc.get("name")));
                history.setCpuPercent(getDoubleValue(proc.get("cpu_percent")));
                history.setMemoryPercent(getDoubleValue(proc.get("memory_percent")));
                history.setStatus(getStringValue(proc.get("status")));
                history.setCreateTime(getStringValue(proc.get("create_time")));
                processHistoryRepository.save(history);
            }
            
            logger.debug("Finished saving {} process entries (current + history).", processes.size());
        } else {
            logger.debug("No process data received for agent: {}", agentId);
        }

        // Port data - dual write
        if (portsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ports = (List<Map<String, Object>>) portsObj;
            logger.debug("---------- Port Data ({} total) ----------", ports.size());
            
            // Save to current status table (replace)
            currentPortInfoRepository.deleteByAgentId(agentId);
            for (Map<String, Object> port : ports) {
                CurrentPortInfo currentInfo = new CurrentPortInfo();
                currentInfo.setAgentId(agentId);
                currentInfo.setPort(getIntValue(port.get("port")));
                currentInfo.setProtocol(getStringValue(port.get("protocol")));
                currentInfo.setStatus(getStringValue(port.get("status")));
                currentInfo.setPid(getIntValue(port.get("pid")));
                currentInfo.setProcessName(getStringValue(port.get("process_name")));
                currentPortInfoRepository.save(currentInfo);
            }
            
            // Save to history table (append)
            for (Map<String, Object> port : ports) {
                PortHistory history = new PortHistory();
                history.setAgentId(agentId);
                history.setCollectionRound(currentRound);
                history.setPort(getIntValue(port.get("port")));
                history.setProtocol(getStringValue(port.get("protocol")));
                history.setStatus(getStringValue(port.get("status")));
                history.setPid(getIntValue(port.get("pid")));
                history.setProcessName(getStringValue(port.get("process_name")));
                portHistoryRepository.save(history);
            }
            
            logger.debug("Finished saving {} port entries (current + history).", ports.size());
        } else {
            logger.debug("No port data received for agent: {}", agentId);
        }

        // Host Info data (current only, no history needed)
        if (hostInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hostInfoData = (Map<String, Object>) hostInfoObj;
            logger.debug("---------- Host Info Data ----------");
            
            hostInfoRepository.deleteByAgentId(agentId);
            
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

            installedSoftwareRepository.deleteByAgentId(agentId);

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

            usbDeviceRepository.deleteByAgentId(agentId);

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

            loginLogRepository.deleteByAgentId(agentId);

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

        try {
            if (processesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> processes = (List<Map<String, Object>>) processesObj;
                anomalyDetectionService.detectAndAlert(agentId, "PROCESS", processes);
            }
            if (portsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ports = (List<Map<String, Object>>) portsObj;
                anomalyDetectionService.detectAndAlert(agentId, "PORT", ports);
            }
            if (usbDevicesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> usbDevices = (List<Map<String, Object>>) usbDevicesObj;
                anomalyDetectionService.detectAndAlert(agentId, "USB", usbDevices);
            }
            if (loginLogsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> loginLogs = (List<Map<String, Object>>) loginLogsObj;
                anomalyDetectionService.detectAndAlert(agentId, "LOGIN", loginLogs);
            }
            if (installedSoftwareObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> software = (List<Map<String, Object>>) installedSoftwareObj;
                anomalyDetectionService.detectAndAlert(agentId, "SOFTWARE", software);
            }
        } catch (Exception e) {
            logger.error("Anomaly detection failed for agent {}: {}", agentId, e.getMessage());
        }

        logger.info("========== Finished saving data for agent: {} ==========", agentId);
    }

    public List<CurrentProcessInfo> getProcesses(String agentId) {
        return currentProcessInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
    }

    public List<CurrentPortInfo> getPorts(String agentId) {
        return currentPortInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
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
        
        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "M/d/yyyy",
            "MM/dd/yyyy",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "yyyy-M-d",
            "M-d-yyyy"
        };
        
        for (String pattern : patterns) {
            try {
                return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // continue to next pattern
            }
        }
        
        logger.warn("Unable to parse date: {}", dateStr);
        return null;
    }

    private java.time.LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String dateTimeStr = value.toString();
        
        String[] patterns = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "M/d/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "M/d/yyyy H:mm:ss",
            "MM/dd/yyyy H:mm:ss"
        };
        
        for (String pattern : patterns) {
            try {
                return java.time.LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // continue to next pattern
            }
        }
        
        logger.warn("Unable to parse datetime: {}", dateTimeStr);
        return null;
    }
}
