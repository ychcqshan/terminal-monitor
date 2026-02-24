package com.monitor.controller;

import com.monitor.service.DataCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class DataCleanupController {

    private final DataCleanupService dataCleanupService;

    public DataCleanupController(DataCleanupService dataCleanupService) {
        this.dataCleanupService = dataCleanupService;
    }

    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> manualCleanup(@RequestBody(required = false) Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            LocalDateTime cutoffDate;
            if (request != null && request.containsKey("days")) {
                int days = Integer.parseInt(request.get("days"));
                cutoffDate = LocalDateTime.now().minusDays(days);
            } else {
                cutoffDate = LocalDateTime.now().minusDays(30);
            }
            
            int deletedProcesses = dataCleanupService.cleanupProcessHistory(cutoffDate);
            int deletedPorts = dataCleanupService.cleanupPortHistory(cutoffDate);
            
            response.put("success", true);
            response.put("message", "Cleanup completed");
            response.put("cutoffDate", cutoffDate.toString());
            response.put("deletedProcesses", deletedProcesses);
            response.put("deletedPorts", deletedPorts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
