package com.monitor.repository;

import com.monitor.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByStatus(String status);
    long countByStatus(String status);
}
