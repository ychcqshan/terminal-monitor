package com.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollectionRoundService {

    private static final Logger logger = LoggerFactory.getLogger(CollectionRoundService.class);

    private final Map<String, Integer> agentRounds = new ConcurrentHashMap<>();

    public int getNextRound(String agentId) {
        int currentRound = agentRounds.getOrDefault(agentId, 0);
        int nextRound = currentRound + 1;
        agentRounds.put(agentId, nextRound);
        logger.debug("Agent {} next collection round: {}", agentId, nextRound);
        return nextRound;
    }

    public int getCurrentRound(String agentId) {
        return agentRounds.getOrDefault(agentId, 0);
    }

    public void resetRound(String agentId) {
        agentRounds.put(agentId, 0);
        logger.info("Agent {} collection round reset to 0", agentId);
    }
}
