package com.monitor.service;

import com.monitor.entity.AlertRule;
import com.monitor.entity.SecurityAlert;
import com.monitor.repository.AlertRuleRepository;
import com.monitor.repository.SecurityAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final SecurityAlertRepository alertRepository;
    private final AlertRuleRepository ruleRepository;

    public AlertService(SecurityAlertRepository alertRepository,
                       AlertRuleRepository ruleRepository) {
        this.alertRepository = alertRepository;
        this.ruleRepository = ruleRepository;
    }

    public SecurityAlert createAlert(String agentId, String alertType, String alertLevel,
                                      String alertTitle, String alertContent, String anomalyType,
                                      String baselineItem, String currentItem) {
        SecurityAlert alert = new SecurityAlert();
        alert.setAgentId(agentId);
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setAlertTitle(alertTitle);
        alert.setAlertContent(alertContent);
        alert.setAnomalyType(anomalyType);
        alert.setBaselineItem(baselineItem);
        alert.setCurrentItem(currentItem);
        alert.setAlertStatus("NEW");
        alert.setCreatedAt(LocalDateTime.now());

        return alertRepository.save(alert);
    }

    public SecurityAlert createSimpleAlert(String agentId, String alertType, String alertLevel,
                                            String alertTitle, String alertContent) {
        return createAlert(agentId, alertType, alertLevel, alertTitle, alertContent, null, null, null);
    }

    public List<SecurityAlert> getAlertsByAgent(String agentId) {
        return alertRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    public List<SecurityAlert> getAlertsByAgentAndStatus(String agentId, String status) {
        return alertRepository.findByAgentIdAndAlertStatus(agentId, status);
    }

    public List<SecurityAlert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SecurityAlert> getAlertsByStatus(String status) {
        return alertRepository.findByAlertStatusOrderByCreatedAtDesc(status);
    }

    public Optional<SecurityAlert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public long countNewAlerts() {
        return alertRepository.countNewAlerts();
    }

    public long countNewAlertsByAgent(String agentId) {
        return alertRepository.countNewAlertsByAgent(agentId);
    }

    public long countCriticalUnresolved() {
        return alertRepository.countCriticalUnresolved();
    }

    @Transactional
    public SecurityAlert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Optional<SecurityAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            SecurityAlert alert = alertOpt.get();
            alert.setAlertStatus("ACKNOWLEDGED");
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(acknowledgedBy);
            logger.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
            return alertRepository.save(alert);
        }
        throw new RuntimeException("Alert not found: " + alertId);
    }

    @Transactional
    public SecurityAlert resolveAlert(Long alertId, String resolvedBy, String resolutionNote) {
        Optional<SecurityAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            SecurityAlert alert = alertOpt.get();
            alert.setAlertStatus("RESOLVED");
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(resolvedBy);
            alert.setResolutionNote(resolutionNote);
            logger.info("Alert {} resolved by {}: {}", alertId, resolvedBy, resolutionNote);
            return alertRepository.save(alert);
        }
        throw new RuntimeException("Alert not found: " + alertId);
    }

    @Transactional
    public SecurityAlert ignoreAlert(Long alertId, String ignoredBy, String ignoreReason) {
        Optional<SecurityAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            SecurityAlert alert = alertOpt.get();
            alert.setAlertStatus("IGNORED");
            alert.setIgnoredAt(LocalDateTime.now());
            alert.setIgnoredBy(ignoredBy);
            alert.setIgnoreReason(ignoreReason);
            logger.info("Alert {} ignored by {}: {}", alertId, ignoredBy, ignoreReason);
            return alertRepository.save(alert);
        }
        throw new RuntimeException("Alert not found: " + alertId);
    }

    public List<SecurityAlert> getRecentAlerts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findRecentAlerts(since);
    }

    public AlertRule createRule(String ruleType, String ruleName, String ruleCondition,
                                 String alertLevel, String actionType, boolean enabled) {
        AlertRule rule = new AlertRule();
        rule.setRuleType(ruleType);
        rule.setRuleName(ruleName);
        rule.setRuleCondition(ruleCondition);
        rule.setAlertLevel(alertLevel);
        rule.setActionType(actionType);
        rule.setEnabled(enabled);
        rule.setCreatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    public List<AlertRule> getEnabledRules() {
        return ruleRepository.findByEnabledTrue();
    }

    public List<AlertRule> getRulesByType(String ruleType) {
        return ruleRepository.findByRuleType(ruleType);
    }

    public List<AlertRule> getAllRules() {
        return ruleRepository.findAll();
    }

    @Transactional
    public AlertRule toggleRule(Long ruleId, boolean enabled) {
        Optional<AlertRule> ruleOpt = ruleRepository.findById(ruleId);
        if (ruleOpt.isPresent()) {
            AlertRule rule = ruleOpt.get();
            rule.setEnabled(enabled);
            return ruleRepository.save(rule);
        }
        throw new RuntimeException("Rule not found: " + ruleId);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    public AlertStats getAlertStats() {
        AlertStats stats = new AlertStats();
        stats.setTotalNew(alertRepository.countNewAlerts());
        stats.setTotalAcknowledged(alertRepository.countByStatus("ACKNOWLEDGED"));
        stats.setTotalResolved(alertRepository.countByStatus("RESOLVED"));
        stats.setTotalIgnored(alertRepository.countByStatus("IGNORED"));
        stats.setCriticalUnresolved(alertRepository.countCriticalUnresolved());
        stats.setTotalAlerts(alertRepository.count());
        return stats;
    }

    public static class AlertStats {
        private long totalNew;
        private long totalAcknowledged;
        private long totalResolved;
        private long totalIgnored;
        private long criticalUnresolved;
        private long totalAlerts;

        public long getTotalNew() { return totalNew; }
        public void setTotalNew(long totalNew) { this.totalNew = totalNew; }
        public long getTotalAcknowledged() { return totalAcknowledged; }
        public void setTotalAcknowledged(long totalAcknowledged) { this.totalAcknowledged = totalAcknowledged; }
        public long getTotalResolved() { return totalResolved; }
        public void setTotalResolved(long totalResolved) { this.totalResolved = totalResolved; }
        public long getTotalIgnored() { return totalIgnored; }
        public void setTotalIgnored(long totalIgnored) { this.totalIgnored = totalIgnored; }
        public long getCriticalUnresolved() { return criticalUnresolved; }
        public void setCriticalUnresolved(long criticalUnresolved) { this.criticalUnresolved = criticalUnresolved; }
        public long getTotalAlerts() { return totalAlerts; }
        public void setTotalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; }
    }
}
