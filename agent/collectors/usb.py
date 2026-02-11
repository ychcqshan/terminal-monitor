"""
USB设备信息采集模块
采集USB设备信息，包括存储设备、键盘、鼠标等
兼容Windows和Linux
"""

import platform
import subprocess
import re
import json
from typing import Dict, List, Optional
from datetime import datetime
from utils.logger import setup_logger

logger = setup_logger()


class USBCollector:
    """USB设备信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> List[Dict]:
        """采集所有USB设备信息"""
        usb_list = []

        try:
            if self.system == "Windows":
                usb_list = self._collect_usb_windows()
            elif self.system == "Linux":
                usb_list = self._collect_usb_linux()
        except Exception as e:
            logger.error(f"[USB设备] 采集失败: {e}")

        logger.info(f"[USB设备] 采集到 {len(usb_list)} 个设备")
        return usb_list

    def _collect_usb_windows(self) -> List[Dict]:
        """Windows平台采集USB设备"""
        usb_list = []
        seen_devices = set()

        try:
            usb_list = self._get_usb_storage_from_registry(usb_list, seen_devices)
            usb_list = self._get_usb_devices_from_pnp(usb_list, seen_devices)

            logger.debug(f"[USB设备] Windows 采集到 {len(usb_list)} 个")
        except Exception as e:
            logger.error(f"[USB设备] Windows采集失败: {e}")

        return usb_list

    def _get_usb_storage_from_registry(self, usb_list: List[Dict], seen_devices: set) -> List[Dict]:
        """从注册表获取USB存储设备详细信息"""
        try:
            ps_script = r"""
            $usbDevices = @()
            Get-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Enum\USBSTOR\*" -ErrorAction SilentlyContinue | ForEach-Object {
                $deviceKey = $_.PSChildName
                $props = @{
                    DeviceKey = $deviceKey
                    FriendlyName = if ($_.FriendlyName) { $_.FriendlyName } else { "USB存储设备" }
                    Mfg = if ($_.Mfg) { $_.Mfg } else { $null }
                    DeviceDesc = if ($_.DeviceDesc) { $_.DeviceDesc } else { $null }
                }
                $usbDevices += $props
            }
            $usbDevices | ConvertTo-Json -Depth 3
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=60
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if not isinstance(data, list):
                        data = [data]

                    for device in data:
                        device_key = device.get("DeviceKey", "")
                        if not device_key:
                            continue

                        parts = device_key.split('&')
                        if len(parts) >= 4:
                            vendor_id = parts[0].upper() if len(parts[0]) == 4 else ""
                            product_id = parts[1].upper() if len(parts[1]) == 4 else ""
                            revision = parts[2] if len(parts[2]) >= 4 else ""
                            serial = parts[3] if len(parts[3]) > 0 else ""

                            device_name = device.get("FriendlyName", device.get("DeviceDesc", "USB存储设备"))
                            manufacturer = device.get("Mfg", "") or self._guess_manufacturer(device_name)

                            key = (device_name, vendor_id, product_id, serial)
                            if key in seen_devices:
                                continue
                            seen_devices.add(key)

                            plugged_time = self._get_usb_plugged_time(device_key)

                            usb_list.append({
                                "name": device_name,
                                "type": "storage",
                                "vendor_id": vendor_id,
                                "product_id": product_id,
                                "serial_number": serial,
                                "manufacturer": manufacturer,
                                "version": revision,
                                "status": "running",
                                "plugged_time": plugged_time
                            })
                except json.JSONDecodeError as e:
                    logger.debug(f"[USB设备] 注册表存储设备 JSON解析失败: {e}")

        except Exception as e:
            logger.debug(f"[USB设备] 注册表存储设备查询失败: {e}")

        return usb_list

    def _get_usb_plugged_time(self, device_key: str) -> str:
        """从注册表获取USB设备首次安装时间"""
        try:
            escaped_key = device_key.replace("\\", "\\\\")
            ps_script = f'''
            $keyPath = "HKLM:\\\\SYSTEM\\\\CurrentControlSet\\\\Enum\\\\USBSTOR\\\\{escaped_key}"
            $key = Get-Item "$keyPath\\*" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($key) {{
                $props = $key.GetValue("FirstInstallDate")
                if ($props) {{
                    $dt = [DateTime]::FromFileTime([long]$props)
                    $dt.ToString("yyyy-MM-dd HH:mm:ss")
                }} else {{
                    $props = $key.GetValue("InstallTime")
                    if ($props) {{
                        $dt = [DateTime]::FromFileTime([long]$props)
                        $dt.ToString("yyyy-MM-dd HH:mm:ss")
                    }}
                }}
            }}
            '''
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                time_str = result.stdout.strip()
                if time_str and re.match(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}', time_str):
                    return time_str
        except Exception as e:
            logger.debug(f"[USB设备] 获取插入时间失败: {e}")

        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def _get_usb_devices_from_pnp(self, usb_list: List[Dict], seen_devices: set) -> List[Dict]:
        """从PnP和注册表获取所有USB设备信息"""
        try:
            ps_script = r"""
            $usbDevices = @()
            Get-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Enum\USB\*" -ErrorAction SilentlyContinue | Where-Object { $_.PSChildName -like 'VID_*' } | ForEach-Object {
                $deviceKey = $_.PSChildName
                $parentId = $_.ParentId
                $deviceDesc = if ($_.DeviceDesc) { $_.DeviceDesc } else { $null }
                $mfg = if ($_.Mfg) { $_.Mfg } else { $null }

                $usbDevices += @{
                    DeviceKey = $deviceKey
                    ParentId = $parentId
                    DeviceDesc = $deviceDesc
                    Mfg = $mfg
                }
            }
            $usbDevices | ConvertTo-Json -Depth 3
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=60
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if not isinstance(data, list):
                        data = [data]

                    for device in data:
                        device_key = device.get("DeviceKey", "")
                        if not device_key or "VID_" not in device_key:
                            continue

                        vid_pid_parts = device_key.split('\\')[0] if "\\" in device_key else device_key
                        parts = vid_pid_parts.split('&')

                        vid = ""
                        pid = ""
                        for part in parts:
                            if part.startswith("VID_"):
                                vid = part.replace("VID_", "").upper()
                            elif part.startswith("PID_"):
                                pid = part.replace("PID_", "").upper()

                        serial = ""
                        if "\\" in device_key:
                            serial = device_key.split("\\")[-1]

                        device_name = device.get("DeviceDesc", "")
                        if not device_name:
                            device_name = device.get("Mfg", "")
                        if not device_name:
                            device_name = f"USB Device ({vid}:{pid})"

                        manufacturer = device.get("Mfg", "") or self._guess_manufacturer(device_name)
                        device_type = self._detect_device_type(device_name, manufacturer, device_key)

                        key = (device_name, vid, pid, serial)
                        if key in seen_devices:
                            continue
                        seen_devices.add(key)

                        plugged_time = self._get_usb_plugged_time(f"VID_{vid}&PID_{pid}&{serial}")

                        usb_list.append({
                            "name": device_name,
                            "type": device_type,
                            "vendor_id": vid,
                            "product_id": pid,
                            "serial_number": serial,
                            "manufacturer": manufacturer,
                            "version": "",
                            "status": "running",
                            "plugged_time": plugged_time
                        })
                except json.JSONDecodeError as e:
                    logger.debug(f"[USB设备] 注册表USB设备 JSON解析失败: {e}")

        except Exception as e:
            logger.debug(f"[USB设备] 注册表USB设备查询失败: {e}")

        return usb_list

    def _parse_vid_pid(self, device_id: str) -> tuple:
        """从设备ID解析VID和PID"""
        if not device_id:
            return "", ""
        try:
            match = re.search(r'VID_([0-9A-Fa-f]+)&PID_([0-9A-Fa-f]+)', device_id)
            if match:
                return match.group(1).upper(), match.group(2).upper()
        except:
            pass
        return "", ""

    def _guess_manufacturer(self, name: str) -> str:
        """根据设备名称猜测制造商"""
        name_lower = name.lower()
        manufacturers = {
            'microsoft': 'Microsoft',
            'logitech': 'Logitech',
            'hp': 'HP',
            'dell': 'Dell',
            'lenovo': 'Lenovo',
            'asus': 'ASUS',
            'acer': 'Acer',
            'samsung': 'Samsung',
            'apple': 'Apple',
            'sandisk': 'SanDisk',
            'kingston': 'Kingston',
            'transcend': 'Transcend',
            'toshiba': 'Toshiba',
            'wd': 'Western Digital',
            'wdc': 'Western Digital',
            'seagate': 'Seagate',
            'intel': 'Intel',
            'realtek': 'Realtek',
            'broadcom': 'Broadcom',
            'tp-link': 'TP-Link',
            'mercusys': 'Mercusys',
            'xiaomi': 'Xiaomi',
            'huawei': 'Huawei'
        }
        for key, brand in manufacturers.items():
            if key in name_lower:
                return brand
        return ""

    def _detect_device_type(self, name: str, manufacturer: str, device_id: str) -> str:
        """检测USB设备类型"""
        combined = f"{name} {manufacturer} {device_id}".lower()

        type_keywords = {
            'storage': ['mass storage', 'usb storage', 'flash drive', 'u盘', '磁盘',
                       'external hard', '移动硬盘', 'usb disk', 'disk drive', 'storage', '移动存储'],
            'keyboard': ['keyboard', '键盘', 'key pad'],
            'mouse': ['mouse', '鼠标', 'pointer', 'trackball', 'pointing'],
            'camera': ['camera', 'cam', 'webcam', '视频', 'photo', 'imaging'],
            'audio': ['audio', 'speaker', 'microphone', '耳机', '音响', 'audio device', 'headset'],
            'hub': ['hub', '集线器', 'usb hub'],
            'phone': ['phone', '手机', 'tablet', '平板', 'iphone', 'android', 'mobile'],
            'printer': ['printer', '打印机', 'scanner', '扫描仪'],
            'network': ['network', 'ethernet', 'adapter', '网卡', '无线网卡', 'lan', 'nic', 'wireless']
        }

        for device_type, keywords in type_keywords.items():
            for keyword in keywords:
                if keyword in combined:
                    return device_type

        return 'other'

    def _collect_usb_linux(self) -> List[Dict]:
        """Linux平台采集USB设备"""
        usb_list = []

        try:
            result = subprocess.run(
                ["lsusb", "-v"],
                capture_output=True, text=True, timeout=60
            )

            if result.returncode == 0:
                usb_list = self._parse_lsusb_output(result.stdout)

            self._add_sysfs_usb_info(usb_list)

            logger.debug(f"[USB设备] Linux 采集到 {len(usb_list)} 个")
        except Exception as e:
            logger.error(f"[USB设备] Linux采集失败: {e}")

        return usb_list

    def _parse_lsusb_output(self, output: str) -> List[Dict]:
        """解析lsusb输出"""
        usb_list = []
        current_device = {}

        for line in output.split('\n'):
            if line.startswith('Bus '):
                if current_device.get('name'):
                    usb_list.append(current_device)
                bus_match = re.match(r'Bus\s+(\d+)\s+Device\s+(\d+)\s+ID\s+([0-9a-fA-F:]+)', line)
                if bus_match:
                    current_device = {
                        'bus': bus_match.group(1),
                        'device': bus_match.group(2),
                        'vid': bus_match.group(3).split(':')[0].upper(),
                        'pid': bus_match.group(3).split(':')[1].upper(),
                        'name': '',
                        'manufacturer': '',
                        'type': 'other',
                        'serial_number': '',
                        'version': '',
                        'status': 'running',
                        'plugged_time': ''
                    }
            elif current_device:
                if 'iManufacturer' in line:
                    manufacturer_match = re.sub(r'iManufacturer\s+\d+\s+(.+)', r'\1', line)
                    current_device['manufacturer'] = manufacturer_match.strip()
                elif 'iProduct' in line:
                    product_match = re.sub(r'iProduct\s+\d+\s+(.+)', r'\1', line)
                    current_device['name'] = product_match.strip()
                    current_device['type'] = self._detect_device_type(
                        current_device['name'],
                        current_device.get('manufacturer', ''),
                        ""
                    )
                elif 'iSerial' in line:
                    serial_match = re.sub(r'iSerial\s+\d+\s+(.+)', r'\1', line)
                    current_device['serial_number'] = serial_match.strip()
                elif 'bInterfaceClass' in line:
                    class_code = re.search(r'bInterfaceClass\s+(\d+)', line)
                    if class_code:
                        current_device['type'] = self._get_usb_class_type(int(class_code.group(1)))

        if current_device.get('name'):
            usb_list.append(current_device)

        return usb_list

    def _add_sysfs_usb_info(self, usb_list: List[Dict]):
        """从sysfs补充USB设备信息"""
        try:
            import os

            usb_path = '/sys/bus/usb/devices'
            if os.path.exists(usb_path):
                for device in os.listdir(usb_path):
                    device_path = os.path.join(usb_path, device)

                    for fname in ['manufacturer', 'product', 'serial']:
                        try:
                            fpath = os.path.join(device_path, fname)
                            if os.path.exists(fpath):
                                with open(fpath, 'r') as f:
                                    value = f.read().strip()
                                    if value:
                                        for usb in usb_list:
                                            if usb.get('serial_number') == value:
                                                usb[fname] = value
                        except:
                            pass
        except Exception as e:
            logger.debug(f"[USB设备] sysfs信息读取失败: {e}")

    def _get_usb_class_type(self, class_code: int) -> str:
        """根据USB类别代码判断设备类型"""
        class_types = {
            0x01: 'audio',
            0x02: 'comm',
            0x03: 'human_interface',
            0x05: 'physical',
            0x06: 'image',
            0x07: 'printer',
            0x08: 'storage',
            0x09: 'hub',
            0x0A: 'cdc',
            0x0B: 'smart_card',
            0x0E: 'video',
            0x0F: 'personal_healthcare',
            0x10: 'audio_video',
            0xE0: 'wireless_controller',
            0xEF: 'miscellaneous',
            0xFF: 'vendor_specific'
        }
        return class_types.get(class_code, 'other')

    def get_usb_storage_devices(self) -> List[Dict]:
        """获取USB存储设备"""
        all_devices = self.collect()
        return [d for d in all_devices if d.get('type') == 'storage']

    def get_recent_usb_devices(self, hours: int = 24) -> List[Dict]:
        """获取最近连接的USB设备"""
        all_devices = self.collect()
        return all_devices
