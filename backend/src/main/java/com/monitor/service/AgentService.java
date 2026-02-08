package com.monitor.service;

import com.monitor.entity.Agent;
import com.monitor.entity.PortInfo;
import com.monitor.entity.ProcessInfo;
import com.monitor.repository.AgentRepository;
import com.monitor.repository.PortInfoRepository;
import com.monitor.repository.ProcessInfoRepository;
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

    public AgentService(AgentRepository agentRepository,
                       ProcessInfoRepository processInfoRepository,
                       PortInfoRepository portInfoRepository) {
        this.agentRepository = agentRepository;
        this.processInfoRepository = processInfoRepository;
        this.portInfoRepository = portInfoRepository;
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
        Object processesObj = data.get("processes");
        Object portsObj = data.get("ports");

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
                // To avoid flooding the database, we can decide whether to save all data or just a snapshot.
                // For now, we save all.
                processInfoRepository.saveAndFlush(processInfo);
            }
            logger.debug("Finished saving {} process entries.", processes.size());
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
