package com.monitor.repository;

import com.monitor.entity.PortBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortBaselineRepository extends JpaRepository<PortBaseline, Long> {
    List<PortBaseline> findByAgentId(String agentId);
    Optional<PortBaseline> findByAgentIdAndPortAndProtocol(String agentId, Integer port, String protocol);
    void deleteByAgentId(String agentId);
}
