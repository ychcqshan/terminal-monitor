"""
系统信息采集模块
采集CPU、内存、存储、主板等主机属性信息
兼容Windows和Linux
"""

import platform
import subprocess
import re
import json
from typing import Dict, List, Optional
from utils.logger import setup_logger

logger = setup_logger()


class SystemCollector:
    """系统信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> Dict:
        """采集完整的系统信息"""
        info = {
            "cpu": self._collect_cpu_info(),
            "memory": self._collect_memory_info(),
            "storage": self._collect_storage_info(),
            "motherboard": self._collect_motherboard_info(),
            "os": self._collect_os_info()
        }
        logger.info(f"[系统信息] CPU: {info['cpu'].get('brand', 'Unknown')}, "
                    f"内存: {info['memory'].get('total_human', 'Unknown')}, "
                    f"磁盘: {len(info['storage'])}个")
        return info

    def _collect_cpu_info(self) -> Dict:
        """采集CPU信息"""
        cpu_info = {
            "brand": "",
            "arch": platform.machine(),
            "cores": 0,
            "threads": 0,
            "frequency": 0.0
        }

        try:
            if self.system == "Windows":
                return self._collect_cpu_windows()
            elif self.system == "Linux":
                return self._collect_cpu_linux()
        except Exception as e:
            logger.error(f"[系统信息] CPU采集失败: {e}")

        return cpu_info

    def _collect_cpu_windows(self) -> Dict:
        """Windows平台采集CPU信息"""
        cpu_info = {
            "brand": "",
            "arch": platform.machine(),
            "cores": 0,
            "threads": 0,
            "frequency": 0.0
        }

        try:
            ps_script = """
            Get-CimInstance Win32_Processor | Select-Object Name,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if isinstance(data, list) and len(data) > 0:
                        data = data[0]
                    if data.get("Name"):
                        cpu_info["brand"] = data["Name"]
                    if data.get("NumberOfCores"):
                        cpu_info["cores"] = int(data["NumberOfCores"])
                    if data.get("NumberOfLogicalProcessors"):
                        cpu_info["threads"] = int(data["NumberOfLogicalProcessors"])
                    if data.get("MaxClockSpeed"):
                        freq_mhz = float(data["MaxClockSpeed"])
                        cpu_info["frequency"] = freq_mhz / 1000.0
                except json.JSONDecodeError as e:
                    logger.error(f"[系统信息] Windows CPU JSON解析失败: {e}")

            logger.debug(f"[系统信息] Windows CPU: {cpu_info['brand']}")
        except Exception as e:
            logger.error(f"[系统信息] Windows CPU采集失败: {e}")

        return cpu_info

    def _collect_cpu_linux(self) -> Dict:
        """Linux平台采集CPU信息"""
        cpu_info = {
            "brand": "",
            "arch": platform.machine(),
            "cores": 0,
            "threads": 0,
            "frequency": 0.0
        }

        try:
            with open('/proc/cpuinfo', 'r') as f:
                cpuinfo = f.read()

            model_match = re.search(r'model name\s*:\s*(.+)', cpuinfo)
            if model_match:
                cpu_info["brand"] = model_match.group(1).strip()

            cores_match = re.search(r'cpu cores\s*:\s*(\d+)', cpuinfo)
            if cores_match:
                cpu_info["cores"] = int(cores_match.group(1))

            threads_match = re.search(r'siblings\s*:\s*(\d+)', cpuinfo)
            if threads_match:
                cpu_info["threads"] = int(threads_match.group(1))

            try:
                with open('/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq', 'r') as f:
                    cpu_info["frequency"] = float(f.read().strip()) / 1000000.0
            except:
                pass

            logger.debug(f"[系统信息] Linux CPU: {cpu_info['brand']}, 核心: {cpu_info['cores']}")
        except Exception as e:
            logger.error(f"[系统信息] Linux CPU采集失败: {e}")

        return cpu_info

    def _collect_memory_info(self) -> Dict:
        """采集内存信息"""
        memory_info = {
            "total": 0,
            "available": 0,
            "percent": 0.0,
            "total_human": "0 GB"
        }

        try:
            if self.system == "Windows":
                return self._collect_memory_windows()
            elif self.system == "Linux":
                return self._collect_memory_linux()
        except Exception as e:
            logger.error(f"[系统信息] 内存采集失败: {e}")

        return memory_info

    def _collect_memory_windows(self) -> Dict:
        """Windows平台采集内存信息"""
        memory_info = {
            "total": 0,
            "available": 0,
            "percent": 0.0,
            "total_human": "0 GB"
        }

        try:
            ps_script = """
            Get-CimInstance Win32_OperatingSystem | Select-Object TotalVisibleMemorySize,FreePhysicalMemory | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if isinstance(data, list) and len(data) > 0:
                        data = data[0]
                    if data.get("TotalVisibleMemorySize"):
                        total_kb = int(data["TotalVisibleMemorySize"])
                        memory_info["total"] = total_kb * 1024
                    if data.get("FreePhysicalMemory"):
                        free_kb = int(data["FreePhysicalMemory"])
                        memory_info["available"] = free_kb * 1024
                    if memory_info["total"] > 0:
                        memory_info["percent"] = round((1 - memory_info["available"] / memory_info["total"]) * 100, 2)
                    memory_info["total_human"] = self._bytes_to_human(memory_info["total"])
                except json.JSONDecodeError as e:
                    logger.error(f"[系统信息] Windows 内存 JSON解析失败: {e}")

            logger.debug(f"[系统信息] Windows 内存: {memory_info['total_human']}")
        except Exception as e:
            logger.error(f"[系统信息] Windows内存采集失败: {e}")

        return memory_info

    def _collect_memory_linux(self) -> Dict:
        """Linux平台采集内存信息"""
        memory_info = {
            "total": 0,
            "available": 0,
            "percent": 0.0,
            "total_human": "0 GB"
        }

        try:
            with open('/proc/meminfo', 'r') as f:
                meminfo = f.read()

            total_match = re.search(r'MemTotal:\s*(\d+)\s*kB', meminfo)
            available_match = re.search(r'MemAvailable:\s*(\d+)\s*kB', meminfo)

            if total_match:
                total_kb = int(total_match.group(1))
                memory_info["total"] = total_kb * 1024
                memory_info["total_human"] = self._bytes_to_human(memory_info["total"])

            if available_match:
                available_kb = int(available_match.group(1))
                memory_info["available"] = available_kb * 1024

            if memory_info["total"] > 0:
                memory_info["percent"] = round(
                    (1 - memory_info["available"] / memory_info["total"]) * 100, 2
                )

            logger.debug(f"[系统信息] Linux 内存: {memory_info['total_human']}")
        except Exception as e:
            logger.error(f"[系统信息] Linux内存采集失败: {e}")

        return memory_info

    def _collect_storage_info(self) -> List[Dict]:
        """采集存储信息"""
        storage_list = []

        try:
            if self.system == "Windows":
                return self._collect_storage_windows()
            elif self.system == "Linux":
                return self._collect_storage_linux()
        except Exception as e:
            logger.error(f"[系统信息] 存储采集失败: {e}")

        return storage_list

    def _collect_storage_windows(self) -> List[Dict]:
        """Windows平台采集存储信息"""
        storage_list = []

        try:
            ps_script = """
            Get-CimInstance Win32_LogicalDisk | Where-Object {$_.DriveType -eq 3} | Select-Object Name,Size,FreeSpace,VolumeName | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if not isinstance(data, list):
                        data = [data]
                    for disk in data:
                        if disk.get("Name") and disk.get("Size"):
                            size = int(disk["Size"])
                            free = int(disk.get("FreeSpace", 0))
                            storage_list.append({
                                "device": disk["Name"],
                                "total": size,
                                "used": size - free,
                                "free": free,
                                "percent": round((1 - free / size) * 100, 2) if size > 0 else 0,
                                "mountpoint": disk["Name"] + "\\"
                            })
                except json.JSONDecodeError as e:
                    logger.error(f"[系统信息] Windows 存储 JSON解析失败: {e}")

            logger.debug(f"[系统信息] Windows 磁盘数: {len(storage_list)}")
        except Exception as e:
            logger.error(f"[系统信息] Windows存储采集失败: {e}")

        return storage_list

    def _collect_storage_linux(self) -> List[Dict]:
        """Linux平台采集存储信息"""
        storage_list = []

        try:
            result = subprocess.run(
                ["df", "-h", "-P", "--output=source,size,used,avail,pcent,file,mountpoint"],
                capture_output=True, text=True, timeout=30
            )
            lines = result.stdout.strip().split('\n')
            for line in lines[1:]:
                if line.strip():
                    parts = line.split()
                    if len(parts) >= 6:
                        try:
                            total = self._human_to_bytes(parts[1])
                            used = self._human_to_bytes(parts[2])
                            avail = self._human_to_bytes(parts[3])
                            percent = float(parts[4].replace('%', ''))
                            mountpoint = parts[-1]

                            storage_list.append({
                                "device": parts[0],
                                "total": total,
                                "used": used,
                                "free": avail,
                                "percent": percent,
                                "mountpoint": mountpoint
                            })
                        except:
                            continue

            logger.debug(f"[系统信息] Linux 磁盘数: {len(storage_list)}")
        except Exception as e:
            logger.error(f"[系统信息] Linux存储采集失败: {e}")

        return storage_list

    def _collect_motherboard_info(self) -> Dict:
        """采集主板信息"""
        motherboard = {
            "model": "",
            "serial": ""
        }

        try:
            if self.system == "Windows":
                return self._collect_motherboard_windows()
            elif self.system == "Linux":
                return self._collect_motherboard_linux()
        except Exception as e:
            logger.error(f"[系统信息] 主板采集失败: {e}")

        return motherboard

    def _collect_motherboard_windows(self) -> Dict:
        """Windows平台采集主板信息"""
        motherboard = {"model": "", "serial": "", "bios_version": ""}

        try:
            ps_script = """
            Get-CimInstance Win32_BaseBoard | Select-Object Product,SerialNumber | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if isinstance(data, list) and len(data) > 0:
                        data = data[0]
                    if data.get("Product"):
                        motherboard["model"] = data["Product"]
                    if data.get("SerialNumber"):
                        motherboard["serial"] = data["SerialNumber"]
                except json.JSONDecodeError as e:
                    logger.error(f"[系统信息] Windows 主板 JSON解析失败: {e}")

            ps_bios_script = """
            Get-CimInstance Win32_BIOS | Select-Object SMBIOSBIOSVersion,ReleaseDate | ConvertTo-Json
            """
            bios_result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_bios_script],
                capture_output=True, text=True, timeout=30
            )

            if bios_result.returncode == 0 and bios_result.stdout.strip():
                try:
                    bios_data = json.loads(bios_result.stdout)
                    if isinstance(bios_data, list) and len(bios_data) > 0:
                        bios_data = bios_data[0]
                    if bios_data.get("SMBIOSBIOSVersion"):
                        motherboard["bios_version"] = str(bios_data["SMBIOSBIOSVersion"])
                except json.JSONDecodeError:
                    pass

            logger.debug(f"[系统信息] Windows 主板: {motherboard['model']}, BIOS: {motherboard['bios_version']}")
        except Exception as e:
            logger.error(f"[系统信息] Windows主板采集失败: {e}")

        return motherboard

    def _collect_motherboard_linux(self) -> Dict:
        """Linux平台采集主板信息"""
        motherboard = {"model": "", "serial": ""}

        try:
            for fname in ['/sys/class/dmi/id/board_name', '/sys/class/dmi/id/board_vendor']:
                try:
                    with open(fname, 'r') as f:
                        if 'name' in fname:
                            motherboard["model"] = f.read().strip()
                        else:
                            motherboard["model"] = f.read().strip()
                except:
                    pass

            for fname in ['/sys/class/dmi/id/board_serial']:
                try:
                    with open(fname, 'r') as f:
                        motherboard["serial"] = f.read().strip()
                except:
                    pass

            logger.debug(f"[系统信息] Linux 主板: {motherboard['model']}")
        except Exception as e:
            logger.error(f"[系统信息] Linux主板采集失败: {e}")

        return motherboard

    def _collect_os_info(self) -> Dict:
        """采集操作系统信息"""
        os_info = {
            "name": platform.system(),
            "version": platform.version(),
            "arch": platform.machine(),
            "kernel": platform.release()
        }

        try:
            if self.system == "Windows":
                ps_script = """
                Get-CimInstance Win32_OperatingSystem | Select-Object Caption,Version,BuildNumber | ConvertTo-Json
                """
                result = subprocess.run(
                    ["powershell", "-NoProfile", "-Command", ps_script],
                    capture_output=True, text=True, timeout=30
                )

                if result.returncode == 0 and result.stdout.strip():
                    try:
                        data = json.loads(result.stdout)
                        if isinstance(data, list) and len(data) > 0:
                            data = data[0]
                        if data.get("Caption"):
                            os_info["name"] = data["Caption"]
                        if data.get("Version"):
                            os_info["version"] = data["Version"]
                        if data.get("BuildNumber"):
                            os_info["kernel"] = str(data["BuildNumber"])
                    except json.JSONDecodeError as e:
                        logger.error(f"[系统信息] Windows OS JSON解析失败: {e}")
            elif self.system == "Linux":
                import os
                with open('/etc/os-release', 'r') as f:
                    for line in f:
                        if line.startswith('PRETTY_NAME='):
                            os_info["name"] = line.split('=')[1].strip().strip('"')
                            break

            logger.debug(f"[系统信息] OS: {os_info['name']} {os_info['version']}")
        except Exception as e:
            logger.error(f"[系统信息] OS采集失败: {e}")

        return os_info

    def _bytes_to_human(self, bytes_num: float) -> str:
        """字节转人类可读格式"""
        for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
            if bytes_num < 1024.0:
                return f"{bytes_num:.1f} {unit}"
            bytes_num /= 1024.0
        return f"{bytes_num:.1f} PB"

    def _human_to_bytes(self, human_str: str) -> int:
        """人类可读格式转字节"""
        units = {'K': 1024, 'M': 1024**2, 'G': 1024**3, 'T': 1024**4}
        try:
            if human_str[-1] in units:
                return int(float(human_str[:-1]) * units[human_str[-1]])
            return int(human_str)
        except:
            return 0
