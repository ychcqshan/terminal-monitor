package com.monitor.repository;

import com.monitor.entity.HostInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HostInfoRepository extends JpaRepository<HostInfo, Long> {

    List<HostInfo> findByAgentIdOrderByCollectedAtDesc(String agentId);

    HostInfo findTopByAgentIdOrderByCollectedAtDesc(String agentId);

    void deleteByAgentId(String agentId);
}
