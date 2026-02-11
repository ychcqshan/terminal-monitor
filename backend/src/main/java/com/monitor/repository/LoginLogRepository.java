package com.monitor.repository;

import com.monitor.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findByAgentIdOrderByLoginTimeDesc(String agentId);

    List<LoginLog> findByAgentIdAndUsername(String agentId, String username);

    List<LoginLog> findByAgentIdAndLoginStatus(String agentId, String loginStatus);

    void deleteByAgentId(String agentId);
}
