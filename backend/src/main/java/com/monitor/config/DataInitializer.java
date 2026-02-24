package com.monitor.config;

import com.monitor.entity.AlertRule;
import com.monitor.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final AlertRuleRepository alertRuleRepository;

    public DataInitializer(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    @Override
    public void run(String... args) {
        initDefaultAlertRules();
    }

    private void initDefaultAlertRules() {
        createRuleIfNotExists(
            "未知进程检测",
            "PROCESS",
            "检测不在基线中的新进程",
            "MEDIUM",
            true
        );

        createRuleIfNotExists(
            "异常端口检测",
            "PORT",
            "检测不在基线中的新端口",
            "HIGH",
            true
        );

        createRuleIfNotExists(
            "USB设备接入检测",
            "USB",
            "检测新接入的USB设备",
            "MEDIUM",
            true
        );

        createRuleIfNotExists(
            "异常登录检测",
            "LOGIN",
            "检测异常登录行为",
            "HIGH",
            true
        );

        createRuleIfNotExists(
            "软件变更检测",
            "SOFTWARE",
            "检测新安装的软件",
            "LOW",
            false
        );

        logger.info("Default alert rules initialized");
    }

    private void createRuleIfNotExists(String ruleName, String ruleType, 
                                        String ruleCondition, String alertLevel, 
                                        boolean enabled) {
        Optional<AlertRule> existing = alertRuleRepository.findByRuleName(ruleName);
        if (existing.isEmpty()) {
            AlertRule rule = new AlertRule();
            rule.setRuleName(ruleName);
            rule.setRuleType(ruleType);
            rule.setRuleCondition(ruleCondition);
            rule.setAlertLevel(alertLevel);
            rule.setActionType("ALERT");
            rule.setEnabled(enabled);
            rule.setCreatedAt(LocalDateTime.now());
            alertRuleRepository.save(rule);
            logger.info("Created default rule: {}", ruleName);
        }
    }
}
