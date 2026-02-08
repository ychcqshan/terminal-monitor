package com.monitor.repository;

import com.monitor.entity.PortInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortInfoRepository extends JpaRepository<PortInfo, Long> {
    List<PortInfo> findByAgentIdOrderByCollectedAtDesc(String agentId);
    List<PortInfo> findByAgentId(String agentId);
    void deleteByAgentId(String agentId);
}
