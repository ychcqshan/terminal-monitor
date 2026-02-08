package com.monitor.controller;

import com.monitor.entity.Agent;
import com.monitor.entity.PortInfo;
import com.monitor.entity.ProcessInfo;
import com.monitor.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> agentInfo) {
        Map<String, Object> response = new HashMap<>();

        try {
            Agent agent = agentService.registerOrUpdateAgent(agentInfo);
            response.put("success", true);
            response.put("agentId", agent.getId());
            response.put("message", "Agent registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{agentId}/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @PathVariable String agentId,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        String status = body.getOrDefault("status", "running");

        boolean success = agentService.updateHeartbeat(agentId, status);
        response.put("success", success);
        response.put("message", success ? "Heartbeat received" : "Agent not found");

        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{agentId}/data")
    public ResponseEntity<Map<String, Object>> uploadData(
            @PathVariable String agentId,
            @RequestBody Map<String, Object> data) {

        Map<String, Object> response = new HashMap<>();

        try {
            agentService.saveMonitorData(agentId, data);
            response.put("success", true);
            response.put("message", "Data received successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Data processing failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAgents() {
        try {
            List<Agent> agents = agentService.getAllAgents();
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<Agent> getAgent(@PathVariable String agentId) {
        return agentService.getAgent(agentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{agentId}/processes")
    public ResponseEntity<?> getProcesses(@PathVariable String agentId) {
        try {
            List<ProcessInfo> processes = agentService.getProcesses(agentId);
            return ResponseEntity.ok(processes);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/{agentId}/ports")
    public ResponseEntity<?> getPorts(@PathVariable String agentId) {
        try {
            List<PortInfo> ports = agentService.getPorts(agentId);
            return ResponseEntity.ok(ports);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            List<Agent> onlineAgents = agentService.getAgentsByStatus("online");
            List<Agent> offlineAgents = agentService.getAgentsByStatus("offline");
            
            Map<String, Object> response = new HashMap<>();
            response.put("online", onlineAgents.size());
            response.put("offline", offlineAgents.size());
            response.put("total", onlineAgents.size() + offlineAgents.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }
}
