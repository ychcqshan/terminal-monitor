package com.monitor.repository;

import com.monitor.entity.PortHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PortHistoryRepository extends JpaRepository<PortHistory, Long> {

    List<PortHistory> findByAgentId(String agentId);

    @Query("SELECT DISTINCT ph.collectionRound FROM PortHistory ph WHERE ph.agentId = :agentId ORDER BY ph.collectionRound DESC")
    List<Integer> findDistinctCollectionRoundsByAgentIdOrderByCollectionRoundDesc(@Param("agentId") String agentId);

    @Query("SELECT ph FROM PortHistory ph WHERE ph.agentId = :agentId AND ph.collectionRound IN :rounds")
    List<PortHistory> findByAgentIdAndCollectionRoundIn(@Param("agentId") String agentId, @Param("rounds") List<Integer> rounds);

    @Query("SELECT DISTINCT ph.collectionRound FROM PortHistory ph WHERE ph.agentId = :agentId ORDER BY ph.collectionRound DESC LIMIT :limit")
    List<Integer> findRecentRounds(@Param("agentId") String agentId, @Param("limit") int limit);

    void deleteByAgentId(String agentId);

    void deleteByAgentIdAndCollectedAtBefore(String agentId, java.time.LocalDateTime before);

    @Modifying
    @Query("DELETE FROM PortHistory ph WHERE ph.collectedAt < :before")
    int deleteAllByCollectedAtBefore(@Param("before") java.time.LocalDateTime before);
}
