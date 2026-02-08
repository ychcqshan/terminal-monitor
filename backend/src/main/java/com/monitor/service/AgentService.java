package com.monitor.service;

import com.monitor.entity.Agent;
import com.monitor.entity.PortInfo;
import com.monitor.entity.ProcessInfo;
import com.monitor.repository.AgentRepository;
import com.monitor.repository.PortInfoRepository;
import com.monitor.repository.ProcessInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentService {

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
        System.out.println("[DEBUG] registerOrUpdateAgent called with agentId: " + agentId);
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
        System.out.println("[DEBUG] Agent saved with id: " + saved.getId());
        return saved;
    }

    public boolean updateHeartbeat(String agentId, String status) {
        System.out.println("[DEBUG] updateHeartbeat called with agentId: " + agentId + ", status: " + status);
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        System.out.println("[DEBUG] Agent found: " + agentOpt.isPresent());
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setStatus(status);
            agent.setUpdatedAt(LocalDateTime.now());
            agentRepository.saveAndFlush(agent);
            System.out.println("[DEBUG] Heartbeat updated successfully for agent: " + agentId);
            return true;
        }
        System.out.println("[DEBUG] Agent not found: " + agentId);
        return false;
    }

    public Optional<Agent> getAgent(String agentId) {
        return agentRepository.findById(agentId);
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
        Object processesObj = data.get("processes");
        Object portsObj = data.get("ports");

        if (processesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> processes = (List<Map<String, Object>>) processesObj;
            for (Map<String, Object> proc : processes) {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.setAgentId(agentId);
                processInfo.setPid(getIntValue(proc.get("pid")));
                processInfo.setName(getStringValue(proc.get("name")));
                processInfo.setCpuPercent(getDoubleValue(proc.get("cpu_percent")));
                processInfo.setMemoryPercent(getDoubleValue(proc.get("memory_percent")));
                processInfo.setStatus(getStringValue(proc.get("status")));
                processInfo.setCreateTime(getStringValue(proc.get("create_time")));
                processInfoRepository.save(processInfo);
            }
        }

        if (portsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ports = (List<Map<String, Object>>) portsObj;
            for (Map<String, Object> port : ports) {
                PortInfo portInfo = new PortInfo();
                portInfo.setAgentId(agentId);
                portInfo.setPort(getIntValue(port.get("port")));
                portInfo.setProtocol(getStringValue(port.get("protocol")));
                portInfo.setStatus(getStringValue(port.get("status")));
                portInfo.setPid(getIntValue(port.get("pid")));
                portInfo.setProcessName(getStringValue(port.get("process_name")));
                portInfoRepository.save(portInfo);
            }
        }
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
