package com.monitor.controller;

import com.monitor.entity.BaselineConfig;
import com.monitor.entity.BaselineItem;
import com.monitor.entity.BaselineSnapshot;
import com.monitor.entity.dto.BaselineCompareResult;
import com.monitor.entity.dto.BaselineItemDTO;
import com.monitor.service.BaselineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/baselines")
@CrossOrigin(origins = "*")
public class BaselineController {

    private static final Logger logger = LoggerFactory.getLogger(BaselineController.class);

    private final BaselineService baselineService;

    public BaselineController(BaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<List<BaselineConfig>> getBaselineConfigs(@PathVariable String agentId) {
        logger.debug("Getting baseline configs for agent: {}", agentId);
        List<BaselineConfig> configs = baselineService.getBaselineConfigs(agentId);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{agentId}/{type}")
    public ResponseEntity<BaselineConfig> getBaselineConfig(@PathVariable String agentId,
                                                             @PathVariable String type) {
        logger.debug("Getting baseline config for agent {} type {}", agentId, type);
        return baselineService.getBaselineConfig(agentId, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{agentId}/{type}/quick-learn")
    public ResponseEntity<BaselineConfig> startQuickLearn(@PathVariable String agentId,
                                                          @PathVariable String type) {
        logger.info("Starting quick learn for agent {} type {}", agentId, type);
        BaselineConfig config = baselineService.startQuickLearn(agentId, type);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{agentId}/{type}/standard-learn")
    public ResponseEntity<BaselineConfig> startStandardLearn(@PathVariable String agentId,
                                                               @PathVariable String type) {
        logger.info("Starting standard learn for agent {} type {}", agentId, type);
        BaselineConfig config = baselineService.startStandardLearn(agentId, type);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{agentId}/{type}/custom-learn")
    public ResponseEntity<BaselineConfig> startCustomLearn(@PathVariable String agentId,
                                                           @PathVariable String type,
                                                           @RequestBody Map<String, Integer> body) {
        int days = body.getOrDefault("days", 7);
        logger.info("Starting custom learn for {} days for agent {} type {}", days, agentId, type);
        BaselineConfig config = baselineService.startCustomLearn(agentId, type, days);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{agentId}/{type}/import")
    public ResponseEntity<BaselineConfig> importFromCurrent(@PathVariable String agentId,
                                                             @PathVariable String type) {
        logger.info("Importing current data as baseline for agent {} type {}", agentId, type);
        BaselineConfig config = baselineService.importFromCurrent(agentId, type);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{agentId}/{type}/copy")
    public ResponseEntity<BaselineConfig> copyFromAgent(@PathVariable String agentId,
                                                         @PathVariable String type,
                                                         @RequestBody Map<String, String> body) {
        String sourceAgentId = body.get("sourceAgentId");
        if (sourceAgentId == null || sourceAgentId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        logger.info("Copying baseline from {} to {} type {}", sourceAgentId, agentId, type);
        BaselineConfig config = baselineService.copyFromAgent(sourceAgentId, agentId, type);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{agentId}/{type}/manual")
    public ResponseEntity<BaselineConfig> manualCreate(@PathVariable String agentId,
                                                       @PathVariable String type,
                                                       @RequestBody List<BaselineItemDTO> items) {
        logger.info("Manual baseline creation for agent {} type {} with {} items", agentId, type, items.size());
        BaselineConfig config = baselineService.manualCreate(agentId, type, items);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{agentId}/{type}/snapshots")
    public ResponseEntity<List<BaselineSnapshot>> getSnapshots(@PathVariable String agentId,
                                                                @PathVariable String type) {
        logger.debug("Getting snapshots for agent {} type {}", agentId, type);
        List<BaselineSnapshot> snapshots = baselineService.getSnapshots(agentId, type);
        return ResponseEntity.ok(snapshots);
    }

    @GetMapping("/{agentId}/{type}/items")
    public ResponseEntity<List<BaselineItem>> getBaselineItems(@PathVariable String agentId,
                                                                @PathVariable String type) {
        logger.debug("Getting baseline items for agent {} type {}", agentId, type);
        List<BaselineItem> items = baselineService.getBaselineItems(agentId, type);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{agentId}/{type}/compare")
    public ResponseEntity<BaselineCompareResult> compareWithBaseline(@PathVariable String agentId,
                                                                     @PathVariable String type) {
        logger.info("Comparing current data with baseline for agent {} type {}", agentId, type);
        BaselineCompareResult result = baselineService.compareWithBaseline(agentId, type);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{agentId}/{type}/complete-learn")
    public ResponseEntity<Void> completeLearning(@PathVariable String agentId,
                                                 @PathVariable String type) {
        logger.info("Completing learning for agent {} type {}", agentId, type);
        baselineService.completeLearning(agentId, type);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{agentId}/{type}")
    public ResponseEntity<Void> deleteBaseline(@PathVariable String agentId,
                                                @PathVariable String type) {
        logger.info("Deleting baseline for agent {} type {}", agentId, type);
        baselineService.deleteBaseline(agentId, type);
        return ResponseEntity.noContent().build();
    }
}
