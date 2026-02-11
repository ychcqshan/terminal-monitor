package com.monitor.service;

import com.monitor.entity.LoginLog;
import com.monitor.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class LoginLogService {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @Transactional
    public LoginLog saveLoginLog(String agentId, Map<String, Object> data) {
        logger.debug("Saving login log for agent: {}", agentId);

        LoginLog loginLog = new LoginLog();
        loginLog.setAgentId(agentId);

        if (data.containsKey("username")) {
            loginLog.setUsername(getStringValue(data.get("username")));
        }
        if (data.containsKey("login_type")) {
            loginLog.setLoginType(getStringValue(data.get("login_type")));
        }
        if (data.containsKey("login_time")) {
            loginLog.setLoginTime(parseLocalDateTime(data.get("login_time")));
        }
        if (data.containsKey("logout_time")) {
            loginLog.setLogoutTime(parseLocalDateTime(data.get("logout_time")));
        }
        if (data.containsKey("login_ip")) {
            loginLog.setLoginIp(getStringValue(data.get("login_ip")));
        }
        if (data.containsKey("login_status")) {
            loginLog.setLoginStatus(getStringValue(data.get("login_status")));
        }
        if (data.containsKey("session_id")) {
            loginLog.setSessionId(getStringValue(data.get("session_id")));
        }
        if (data.containsKey("source")) {
            loginLog.setSource(getStringValue(data.get("source")));
        }

        LoginLog saved = loginLogRepository.saveAndFlush(loginLog);
        logger.debug("Login log saved with id: {}", saved.getId());
        return saved;
    }

    public List<LoginLog> getLoginLogsHistory(String agentId) {
        return loginLogRepository.findByAgentIdOrderByLoginTimeDesc(agentId);
    }

    public List<LoginLog> getLoginLogsByUsername(String agentId, String username) {
        return loginLogRepository.findByAgentIdAndUsername(agentId, username);
    }

    public List<LoginLog> getLoginLogsByStatus(String agentId, String loginStatus) {
        return loginLogRepository.findByAgentIdAndLoginStatus(agentId, loginStatus);
    }

    @Transactional
    public void deleteByAgentId(String agentId) {
        loginLogRepository.deleteByAgentId(agentId);
        logger.info("Deleted login logs for agent: {}", agentId);
    }

    private String getStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String dateTimeStr = value.toString();
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                logger.warn("Unable to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
    }
}
