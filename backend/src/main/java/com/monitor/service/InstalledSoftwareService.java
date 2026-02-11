package com.monitor.service;

import com.monitor.entity.InstalledSoftware;
import com.monitor.repository.InstalledSoftwareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class InstalledSoftwareService {

    private static final Logger logger = LoggerFactory.getLogger(InstalledSoftwareService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InstalledSoftwareRepository installedSoftwareRepository;

    public InstalledSoftwareService(InstalledSoftwareRepository installedSoftwareRepository) {
        this.installedSoftwareRepository = installedSoftwareRepository;
    }

    @Transactional
    public InstalledSoftware saveInstalledSoftware(String agentId, Map<String, Object> data) {
        logger.debug("Saving installed software for agent: {}", agentId);

        InstalledSoftware software = new InstalledSoftware();
        software.setAgentId(agentId);

        if (data.containsKey("software_name")) {
            software.setSoftwareName(getStringValue(data.get("software_name")));
        }
        if (data.containsKey("version")) {
            software.setVersion(getStringValue(data.get("version")));
        }
        if (data.containsKey("publisher")) {
            software.setPublisher(getStringValue(data.get("publisher")));
        }
        if (data.containsKey("install_date")) {
            software.setInstallDate(parseLocalDate(data.get("install_date")));
        }
        if (data.containsKey("install_location")) {
            software.setInstallLocation(getStringValue(data.get("install_location")));
        }
        if (data.containsKey("size")) {
            software.setSize(getIntValue(data.get("size")));
        }
        if (data.containsKey("software_type")) {
            software.setSoftwareType(getStringValue(data.get("software_type")));
        }
        if (data.containsKey("source")) {
            software.setSource(getStringValue(data.get("source")));
        }

        InstalledSoftware saved = installedSoftwareRepository.saveAndFlush(software);
        logger.debug("Installed software saved with id: {}", saved.getId());
        return saved;
    }

    public List<InstalledSoftware> getInstalledSoftwareHistory(String agentId) {
        return installedSoftwareRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
    }

    public List<InstalledSoftware> getInstalledSoftwareByType(String agentId, String softwareType) {
        return installedSoftwareRepository.findByAgentIdAndSoftwareType(agentId, softwareType);
    }

    public long countInstalledSoftware(String agentId) {
        return installedSoftwareRepository.countByAgentId(agentId);
    }

    @Transactional
    public void deleteByAgentId(String agentId) {
        installedSoftwareRepository.deleteByAgentId(agentId);
        logger.info("Deleted installed software for agent: {}", agentId);
    }

    private Integer getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private String getStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String dateStr = value.toString();
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex) {
                logger.warn("Unable to parse date: {}", dateStr);
                return null;
            }
        }
    }
}
