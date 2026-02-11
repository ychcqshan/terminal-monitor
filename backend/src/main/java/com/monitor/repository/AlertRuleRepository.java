package com.monitor.repository;

import com.monitor.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByEnabledTrue();

    List<AlertRule> findByEnabled(Boolean enabled);

    List<AlertRule> findByRuleTypeAndEnabled(String ruleType, Boolean enabled);

    Optional<AlertRule> findByRuleName(String ruleName);

    List<AlertRule> findByRuleType(String ruleType);
}
