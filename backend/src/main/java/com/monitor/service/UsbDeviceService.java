package com.monitor.service;

import com.monitor.entity.UsbDevice;
import com.monitor.repository.UsbDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class UsbDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(UsbDeviceService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UsbDeviceRepository usbDeviceRepository;

    public UsbDeviceService(UsbDeviceRepository usbDeviceRepository) {
        this.usbDeviceRepository = usbDeviceRepository;
    }

    @Transactional
    public UsbDevice saveUsbDevice(String agentId, Map<String, Object> data) {
        logger.debug("Saving USB device for agent: {}", agentId);

        UsbDevice usbDevice = new UsbDevice();
        usbDevice.setAgentId(agentId);

        if (data.containsKey("device_name")) {
            usbDevice.setDeviceName(getStringValue(data.get("device_name")));
        }
        if (data.containsKey("device_type")) {
            usbDevice.setDeviceType(getStringValue(data.get("device_type")));
        }
        if (data.containsKey("vendor_id")) {
            usbDevice.setVendorId(getStringValue(data.get("vendor_id")));
        }
        if (data.containsKey("product_id")) {
            usbDevice.setProductId(getStringValue(data.get("product_id")));
        }
        if (data.containsKey("serial_number")) {
            usbDevice.setSerialNumber(getStringValue(data.get("serial_number")));
        }
        if (data.containsKey("manufacturer")) {
            usbDevice.setManufacturer(getStringValue(data.get("manufacturer")));
        }
        if (data.containsKey("plugged_time")) {
            usbDevice.setPluggedTime(parseLocalDateTime(data.get("plugged_time")));
        }

        UsbDevice saved = usbDeviceRepository.saveAndFlush(usbDevice);
        logger.debug("USB device saved with id: {}", saved.getId());
        return saved;
    }

    public List<UsbDevice> getUsbDevicesHistory(String agentId) {
        return usbDeviceRepository.findByAgentIdOrderByCollectedAtDesc(agentId);
    }

    public List<UsbDevice> getUsbDevicesByType(String agentId, String deviceType) {
        return usbDeviceRepository.findByAgentIdAndDeviceType(agentId, deviceType);
    }

    @Transactional
    public void deleteByAgentId(String agentId) {
        usbDeviceRepository.deleteByAgentId(agentId);
        logger.info("Deleted USB devices for agent: {}", agentId);
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
