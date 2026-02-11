package com.monitor.controller;

import com.monitor.entity.Agent;
import com.monitor.entity.HostInfo;
import com.monitor.entity.PortInfo;
import com.monitor.entity.ProcessInfo;
import com.monitor.entity.InstalledSoftware;
import com.monitor.entity.UsbDevice;
import com.monitor.entity.LoginLog;
import com.monitor.service.AgentService;
import com.monitor.service.HostInfoService;
import com.monitor.service.InstalledSoftwareService;
import com.monitor.service.UsbDeviceService;
import com.monitor.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final HostInfoService hostInfoService;
    private final InstalledSoftwareService installedSoftwareService;
    private final UsbDeviceService usbDeviceService;
    private final LoginLogService loginLogService;

    public AgentController(AgentService agentService,
                          HostInfoService hostInfoService,
                          InstalledSoftwareService installedSoftwareService,
                          UsbDeviceService usbDeviceService,
                          LoginLogService loginLogService) {
        this.agentService = agentService;
        this.hostInfoService = hostInfoService;
        this.installedSoftwareService = installedSoftwareService;
        this.usbDeviceService = usbDeviceService;
        this.loginLogService = loginLogService;
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
        String status = body.getOrDefault("status", "online");

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
        logger.info("Received data upload request from agentId: {}", agentId);

        try {
            agentService.saveMonitorData(agentId, data);
            response.put("success", true);
            response.put("message", "Data received successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Data processing failed for agentId: {}", agentId, e);
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

    @PutMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> updateAgent(
            @PathVariable String agentId,
            @RequestBody Map<String, String> agentInfo) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = agentService.updateAgent(agentId, agentInfo);
            response.put("success", success);
            response.put("message", success ? "Agent updated successfully" : "Agent not found");
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Update failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> deleteAgent(@PathVariable String agentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = agentService.deleteAgent(agentId);
            response.put("success", success);
            response.put("message", success ? "Agent deleted successfully" : "Agent not found");
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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

    @GetMapping("/{agentId}/host-info")
    public ResponseEntity<?> getHostInfo(@PathVariable String agentId) {
        try {
            List<HostInfo> hostInfo = hostInfoService.getHostInfoHistory(agentId);
            return ResponseEntity.ok(hostInfo);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/{agentId}/installed-software")
    public ResponseEntity<?> getInstalledSoftware(@PathVariable String agentId) {
        try {
            List<InstalledSoftware> installedSoftware = installedSoftwareService.getInstalledSoftwareHistory(agentId);
            return ResponseEntity.ok(installedSoftware);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/{agentId}/usb-devices")
    public ResponseEntity<?> getUsbDevices(@PathVariable String agentId) {
        try {
            List<UsbDevice> usbDevices = usbDeviceService.getUsbDevicesHistory(agentId);
            return ResponseEntity.ok(usbDevices);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @GetMapping("/{agentId}/login-logs")
    public ResponseEntity<?> getLoginLogs(@PathVariable String agentId) {
        try {
            List<LoginLog> loginLogs = loginLogService.getLoginLogsHistory(agentId);
            return ResponseEntity.ok(loginLogs);
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
