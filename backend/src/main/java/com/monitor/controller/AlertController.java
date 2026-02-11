package com.monitor.controller;

import com.monitor.entity.AlertRule;
import com.monitor.entity.SecurityAlert;
import com.monitor.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<SecurityAlert>> getAllAlerts() {
        logger.debug("Getting all alerts");
        List<SecurityAlert> alerts = alertService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SecurityAlert>> getAlertsByStatus(@PathVariable String status) {
        logger.debug("Getting alerts with status: {}", status);
        List<SecurityAlert> alerts = alertService.getAlertsByStatus(status);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/stats")
    public ResponseEntity<AlertService.AlertStats> getAlertStats() {
        logger.debug("Getting alert statistics");
        AlertService.AlertStats stats = alertService.getAlertStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SecurityAlert>> getRecentAlerts(@RequestParam(defaultValue = "24") int hours) {
        logger.debug("Getting recent alerts from last {} hours", hours);
        List<SecurityAlert> alerts = alertService.getRecentAlerts(hours);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecurityAlert> getAlertById(@PathVariable Long id) {
        logger.debug("Getting alert by id: {}", id);
        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<SecurityAlert>> getAlertsByAgent(@PathVariable String agentId) {
        logger.debug("Getting alerts for agent: {}", agentId);
        List<SecurityAlert> alerts = alertService.getAlertsByAgent(agentId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/agent/{agentId}/status/{status}")
    public ResponseEntity<List<SecurityAlert>> getAlertsByAgentAndStatus(@PathVariable String agentId,
                                                                          @PathVariable String status) {
        logger.debug("Getting alerts for agent {} with status {}", agentId, status);
        List<SecurityAlert> alerts = alertService.getAlertsByAgentAndStatus(agentId, status);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<SecurityAlert> acknowledgeAlert(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        String acknowledgedBy = body.getOrDefault("acknowledgedBy", "admin");
        logger.info("Acknowledging alert {} by {}", id, acknowledgedBy);
        try {
            SecurityAlert alert = alertService.acknowledgeAlert(id, acknowledgedBy);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<SecurityAlert> resolveAlert(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String resolvedBy = body.getOrDefault("resolvedBy", "admin");
        String resolutionNote = body.getOrDefault("resolutionNote", "");
        logger.info("Resolving alert {} by {}", id, resolvedBy);
        try {
            SecurityAlert alert = alertService.resolveAlert(id, resolvedBy, resolutionNote);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/ignore")
    public ResponseEntity<SecurityAlert> ignoreAlert(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        String ignoredBy = body.getOrDefault("ignoredBy", "admin");
        String ignoreReason = body.getOrDefault("reason", "");
        logger.info("Ignoring alert {} by {}", id, ignoredBy);
        try {
            SecurityAlert alert = alertService.ignoreAlert(id, ignoredBy, ignoreReason);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRule>> getAllRules() {
        logger.debug("Getting all alert rules");
        List<AlertRule> rules = alertService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/enabled")
    public ResponseEntity<List<AlertRule>> getEnabledRules() {
        logger.debug("Getting enabled alert rules");
        List<AlertRule> rules = alertService.getEnabledRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/type/{ruleType}")
    public ResponseEntity<List<AlertRule>> getRulesByType(@PathVariable String ruleType) {
        logger.debug("Getting alert rules by type: {}", ruleType);
        List<AlertRule> rules = alertService.getRulesByType(ruleType);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRule> createRule(@RequestBody Map<String, Object> body) {
        String ruleType = (String) body.get("ruleType");
        String ruleName = (String) body.get("ruleName");
        String ruleCondition = (String) body.get("ruleCondition");
        String alertLevel = (String) body.getOrDefault("alertLevel", "MEDIUM");
        String actionType = (String) body.getOrDefault("actionType", "ALERT");
        Boolean enabled = (Boolean) body.getOrDefault("enabled", true);

        logger.info("Creating alert rule: {}", ruleName);
        AlertRule rule = alertService.createRule(ruleType, ruleName, ruleCondition, alertLevel, actionType, enabled);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<AlertRule> toggleRule(@PathVariable Long id,
                                                 @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        logger.info("Toggling rule {} to {}", id, enabled);
        try {
            AlertRule rule = alertService.toggleRule(id, enabled);
            return ResponseEntity.ok(rule);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        logger.info("Deleting alert rule: {}", id);
        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
