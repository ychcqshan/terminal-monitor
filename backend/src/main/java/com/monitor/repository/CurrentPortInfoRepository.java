package com.monitor.repository;

import com.monitor.entity.CurrentPortInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentPortInfoRepository extends JpaRepository<CurrentPortInfo, Long> {
    List<CurrentPortInfo> findByAgentIdOrderByCollectedAtDesc(String agentId);
    void deleteByAgentId(String agentId);
}
